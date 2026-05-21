package com.api.financeiro.service;

import com.api.financeiro.client.GatewayClient;
import com.api.financeiro.client.dto.DadosFinanceirosExternos;
import com.api.financeiro.client.dto.ProfissionalExternoDto;
import com.api.financeiro.client.dto.ProjetoExternoDto;
import com.api.financeiro.client.dto.RegistroHoraExternoDto;
import com.api.financeiro.client.dto.TarefaExternaDto;
import com.api.financeiro.dto.response.DashboardFinanceiroResponse;
import com.api.financeiro.dto.response.ProfissionalGanhosResponse;
import com.api.financeiro.dto.response.ProfissionalProjetoResponse;
import com.api.financeiro.dto.response.ProjetoDetalheResponse;
import com.api.financeiro.dto.response.ProjetoFinanceiroResponse;
import com.api.financeiro.dto.response.ProjetoProfissionalResponse;
import com.api.financeiro.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FinanceiroServiceImpl implements FinanceiroService {

    private final GatewayClient gatewayClient;

    public FinanceiroServiceImpl(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @Override
    public List<ProjetoFinanceiroResponse> listarProjetosFinanceiro(String authorization, Integer ano, Integer mes) {
        PeriodoMensal periodo = resolverPeriodoMensal(ano, mes);
        DadosFinanceiros dados = carregarDados(authorization, periodo);

        return dados.projetos().stream()
                .filter(projeto -> projeto.id() != null)
                .map(projeto -> criarProjetoFinanceiroResponse(projeto, tarefasDoProjeto(dados.tarefas(), projeto.id()), dados.registrosPorTarefaId()))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ProjetoFinanceiroResponse::nomeProjeto, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    @Override
    public ProjetoDetalheResponse detalharProjeto(Integer projetoId, String authorization, Integer ano, Integer mes) {
        PeriodoMensal periodo = resolverPeriodoMensal(ano, mes);
        DadosFinanceiros dados = carregarDados(authorization, periodo);
        ProjetoExternoDto projeto = dados.projetosPorId().get(projetoId);

        if (projeto == null) {
            throw new RecursoNaoEncontradoException("Projeto não encontrado id=" + projetoId);
        }

        List<TarefaExternaDto> tarefasProjeto = tarefasDoProjeto(dados.tarefas(), projetoId);

        Map<Integer, List<TarefaExternaDto>> tarefasPorProfissional = tarefasProjeto.stream()
                .filter(tarefa -> tarefa.responsavelId() != null)
                .filter(tarefa -> profissionalAtivo(dados.profissionaisPorId().get(toInteger(tarefa.responsavelId()))))
                .collect(Collectors.groupingBy(
                        tarefa -> toInteger(tarefa.responsavelId()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        if (tarefasPorProfissional.isEmpty()) {
            throw new RecursoNaoEncontradoException("Nenhum dado encontrado para o projeto id=" + projetoId + " no mês selecionado");
        }

        BigDecimal valorHoraProjeto = valorHoraProjeto(projeto);

        List<ProfissionalProjetoResponse> profissionais = tarefasPorProfissional.entrySet().stream()
                .map(entry -> {
                    Integer usuarioId = entry.getKey();
                    ProfissionalExternoDto profissional = dados.profissionaisPorId().get(usuarioId);
                    BigDecimal horas = horasDasTarefas(entry.getValue(), dados.registrosPorTarefaId());
                    BigDecimal valorBase = valorHoraProjeto.multiply(horas).setScale(2, RoundingMode.HALF_UP);

                    return new ProfissionalProjetoResponse(
                            usuarioId,
                            nomeProfissional(profissional, usuarioId),
                            horas,
                            valorHoraProjeto,
                            valorBase
                    );
                })
                .sorted(Comparator.comparing(ProfissionalProjetoResponse::usuarioNome, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        BigDecimal totalHoras = profissionais.stream()
                .map(ProfissionalProjetoResponse::horasTrabalhadas)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal custoTotal = profissionais.stream()
                .map(ProfissionalProjetoResponse::valorBaseCalculado)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new ProjetoDetalheResponse(
                projeto.id(),
                projeto.nome(),
                stringSeguro(projeto.tipoProjeto()),
                totalHoras,
                custoTotal,
                valorHoraProjeto,
                profissionais.size(),
                profissionais
        );
    }

    @Override
    public ProfissionalGanhosResponse detalharGanhosProfissional(Integer usuarioId, BigDecimal bonus, String authorization, Integer ano, Integer mes) {
        PeriodoMensal periodo = resolverPeriodoMensal(ano, mes);
        DadosFinanceiros dados = carregarDados(authorization, periodo);
        ProfissionalExternoDto profissional = dados.profissionaisPorId().get(usuarioId);

        if (!profissionalAtivo(profissional)) {
            throw new RecursoNaoEncontradoException("Nenhum apontamento encontrado para o usuário id=" + usuarioId);
        }

        List<TarefaExternaDto> tarefasProfissional = dados.tarefas().stream()
                .filter(tarefa -> usuarioId.equals(toInteger(tarefa.responsavelId())))
                .toList();

        List<ProjetoProfissionalResponse> projetos = projetosDoProfissional(tarefasProfissional, dados);

        if (projetos.isEmpty()) {
            throw new RecursoNaoEncontradoException("Nenhum apontamento encontrado para o usuário id=" + usuarioId + " no mês selecionado");
        }

        BigDecimal bonusSeguro = normalizarBonus(bonus);
        BigDecimal totalSemBonus = projetos.stream()
                .map(ProjetoProfissionalResponse::valorBaseCalculado)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalComBonus = totalSemBonus.add(bonusSeguro).setScale(2, RoundingMode.HALF_UP);

        return new ProfissionalGanhosResponse(
                usuarioId,
                nomeProfissional(profissional, usuarioId),
                projetos,
                totalSemBonus,
                bonusSeguro,
                totalComBonus
        );
    }

    @Override
    public List<ProfissionalGanhosResponse> listarTodosProfissionais(String authorization, Integer ano, Integer mes) {
        PeriodoMensal periodo = resolverPeriodoMensal(ano, mes);
        DadosFinanceiros dados = carregarDados(authorization, periodo);

        Map<Integer, List<TarefaExternaDto>> tarefasPorProfissional = dados.tarefas().stream()
                .filter(tarefa -> tarefa.responsavelId() != null)
                .filter(tarefa -> profissionalAtivo(dados.profissionaisPorId().get(toInteger(tarefa.responsavelId()))))
                .collect(Collectors.groupingBy(
                        tarefa -> toInteger(tarefa.responsavelId()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return tarefasPorProfissional.entrySet().stream()
                .map(entry -> criarProfissionalGanhosResponse(entry.getKey(), entry.getValue(), dados))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ProfissionalGanhosResponse::usuarioNome, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    @Override
    public DashboardFinanceiroResponse obterDadosDashboard(String authorization, Integer ano, Integer mes) {
        PeriodoMensal periodo = resolverPeriodoMensal(ano, mes);
        DadosFinanceiros dados = carregarDados(authorization, periodo);

        BigDecimal totalHoras = minutosParaHoras(
                dados.registros().stream()
                        .mapToLong(this::minutosDoRegistro)
                        .sum()
        );

        List<TarefaExternaDto> tarefasComHoras = dados.tarefas().stream()
                .filter(tarefa -> tarefa.id() != null)
                .filter(tarefa -> dados.registrosPorTarefaId().containsKey(tarefa.id()))
                .toList();

        BigDecimal custoTotal = tarefasComHoras.stream()
                .map(tarefa -> {
                    ProjetoExternoDto projeto = dados.projetosPorId().get(toInteger(tarefa.projetoId()));
                    if (projeto == null) {
                        return BigDecimal.ZERO;
                    }

                    BigDecimal horas = horasDaTarefa(tarefa.id(), dados.registrosPorTarefaId());
                    return horas.multiply(valorHoraProjeto(projeto));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Long totalProjetos = tarefasComHoras.stream()
                .map(TarefaExternaDto::projetoId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        Long tarefasConcluidas = tarefasComHoras.stream()
                .filter(this::tarefaConcluida)
                .count();

        Map<Long, List<TarefaExternaDto>> tarefasPorProjeto = tarefasComHoras.stream()
                .filter(tarefa -> tarefa.projetoId() != null)
                .collect(Collectors.groupingBy(TarefaExternaDto::projetoId));

        Long projetosConcluidos = tarefasPorProjeto.values().stream()
                .filter(lista -> !lista.isEmpty())
                .filter(lista -> lista.stream().allMatch(this::tarefaConcluida))
                .count();

        Long totalDesenvolvedores = tarefasComHoras.stream()
                .map(TarefaExternaDto::responsavelId)
                .filter(Objects::nonNull)
                .map(this::toInteger)
                .filter(id -> profissionalAtivo(dados.profissionaisPorId().get(id)))
                .distinct()
                .count();

        return new DashboardFinanceiroResponse(
                totalHoras,
                custoTotal,
                totalProjetos,
                tarefasConcluidas,
                projetosConcluidos,
                totalDesenvolvedores
        );
    }

    private ProjetoFinanceiroResponse criarProjetoFinanceiroResponse(
            ProjetoExternoDto projeto,
            List<TarefaExternaDto> tarefasProjeto,
            Map<Long, List<RegistroHoraExternoDto>> registrosPorTarefaId
    ) {
        BigDecimal totalHoras = horasDasTarefas(tarefasProjeto, registrosPorTarefaId);

        if (totalHoras.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal custoTotal = totalHoras.multiply(valorHoraProjeto(projeto)).setScale(2, RoundingMode.HALF_UP);

        return new ProjetoFinanceiroResponse(
                projeto.id(),
                projeto.nome(),
                stringSeguro(projeto.tipoProjeto()),
                totalHoras,
                custoTotal
        );
    }

    private ProfissionalGanhosResponse criarProfissionalGanhosResponse(
            Integer usuarioId,
            List<TarefaExternaDto> tarefasProfissional,
            DadosFinanceiros dados
    ) {
        ProfissionalExternoDto profissional = dados.profissionaisPorId().get(usuarioId);
        List<ProjetoProfissionalResponse> projetos = projetosDoProfissional(tarefasProfissional, dados);

        if (projetos.isEmpty()) {
            return null;
        }

        BigDecimal totalSemBonus = projetos.stream()
                .map(ProjetoProfissionalResponse::valorBaseCalculado)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new ProfissionalGanhosResponse(
                usuarioId,
                nomeProfissional(profissional, usuarioId),
                projetos,
                totalSemBonus,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                totalSemBonus
        );
    }

    private List<ProjetoProfissionalResponse> projetosDoProfissional(List<TarefaExternaDto> tarefasProfissional, DadosFinanceiros dados) {
        Map<Integer, List<TarefaExternaDto>> tarefasPorProjeto = tarefasProfissional.stream()
                .filter(tarefa -> tarefa.projetoId() != null)
                .filter(tarefa -> dados.projetosPorId().containsKey(toInteger(tarefa.projetoId())))
                .collect(Collectors.groupingBy(
                        tarefa -> toInteger(tarefa.projetoId()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return tarefasPorProjeto.entrySet().stream()
                .map(entry -> {
                    ProjetoExternoDto projeto = dados.projetosPorId().get(entry.getKey());
                    BigDecimal horas = horasDasTarefas(entry.getValue(), dados.registrosPorTarefaId());

                    if (horas.compareTo(BigDecimal.ZERO) == 0) {
                        return null;
                    }

                    BigDecimal valorHoraProjeto = valorHoraProjeto(projeto);
                    BigDecimal valorBase = horas.multiply(valorHoraProjeto).setScale(2, RoundingMode.HALF_UP);

                    return new ProjetoProfissionalResponse(
                            projeto.id(),
                            projeto.nome(),
                            horas,
                            valorHoraProjeto,
                            valorBase
                    );
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ProjetoProfissionalResponse::nomeProjeto, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private DadosFinanceiros carregarDados(String authorization, PeriodoMensal periodo) {
        DadosFinanceirosExternos dadosExternos = gatewayClient.buscarDadosFinanceiros(authorization);

        List<ProjetoExternoDto> projetos = listaSegura(dadosExternos.projetos());
        List<TarefaExternaDto> tarefas = listaSegura(dadosExternos.tarefas());
        List<ProfissionalExternoDto> profissionais = listaSegura(dadosExternos.profissionais());

        List<RegistroHoraExternoDto> registros = listaSegura(dadosExternos.registros()).stream()
                .filter(registro -> registroNoPeriodo(registro, periodo))
                .toList();

        Map<Integer, ProjetoExternoDto> projetosPorId = projetos.stream()
                .filter(projeto -> projeto.id() != null)
                .collect(Collectors.toMap(
                        ProjetoExternoDto::id,
                        Function.identity(),
                        (primeiro, segundo) -> primeiro,
                        LinkedHashMap::new
                ));

        Map<Integer, ProfissionalExternoDto> profissionaisPorId = profissionais.stream()
                .filter(profissional -> profissional.id() != null)
                .collect(Collectors.toMap(
                        ProfissionalExternoDto::id,
                        Function.identity(),
                        (primeiro, segundo) -> primeiro,
                        LinkedHashMap::new
                ));

        Map<Long, List<RegistroHoraExternoDto>> registrosPorTarefaId = registros.stream()
                .filter(registro -> registro.tarefaId() != null)
                .collect(Collectors.groupingBy(
                        RegistroHoraExternoDto::tarefaId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return new DadosFinanceiros(
                projetos,
                tarefas,
                registros,
                profissionais,
                projetosPorId,
                profissionaisPorId,
                registrosPorTarefaId
        );
    }

    private boolean registroNoPeriodo(RegistroHoraExternoDto registro, PeriodoMensal periodo) {
        if (registro == null || registro.dataInicio() == null) {
            return false;
        }

        return !registro.dataInicio().isBefore(periodo.inicio()) && registro.dataInicio().isBefore(periodo.fim());
    }

    private PeriodoMensal resolverPeriodoMensal(Integer ano, Integer mes) {
        YearMonth atual = YearMonth.now(ZoneId.systemDefault());
        YearMonth competencia = (ano == null || mes == null)
                ? atual
                : YearMonth.of(ano, mes);

        if (competencia.isAfter(atual)) {
            throw new IllegalArgumentException("Não é permitido consultar mês futuro.");
        }

        Instant inicio = competencia.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant fim = competencia.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        return new PeriodoMensal(inicio, fim);
    }

    private List<TarefaExternaDto> tarefasDoProjeto(List<TarefaExternaDto> tarefas, Integer projetoId) {
        return tarefas.stream()
                .filter(tarefa -> projetoId.equals(toInteger(tarefa.projetoId())))
                .toList();
    }

    private BigDecimal horasDasTarefas(List<TarefaExternaDto> tarefas, Map<Long, List<RegistroHoraExternoDto>> registrosPorTarefaId) {
        long totalMinutos = tarefas.stream()
                .map(TarefaExternaDto::id)
                .filter(Objects::nonNull)
                .map(registrosPorTarefaId::get)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .mapToLong(this::minutosDoRegistro)
                .sum();

        return minutosParaHoras(totalMinutos);
    }

    private BigDecimal horasDaTarefa(Long tarefaId, Map<Long, List<RegistroHoraExternoDto>> registrosPorTarefaId) {
        if (tarefaId == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        long totalMinutos = registrosPorTarefaId.getOrDefault(tarefaId, List.of())
                .stream()
                .mapToLong(this::minutosDoRegistro)
                .sum();

        return minutosParaHoras(totalMinutos);
    }

    private long minutosDoRegistro(RegistroHoraExternoDto registro) {
        if (registro == null) {
            return 0L;
        }

        if (registro.tempoMinutos() != null) {
            return Math.max(registro.tempoMinutos(), 0L);
        }

        if (registro.dataInicio() == null || registro.dataFim() == null || registro.dataFim().isBefore(registro.dataInicio())) {
            return 0L;
        }

        return Math.max(Duration.between(registro.dataInicio(), registro.dataFim()).toMinutes(), 0L);
    }

    private BigDecimal minutosParaHoras(long minutos) {
        return BigDecimal.valueOf(minutos)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal valorHoraProjeto(ProjetoExternoDto projeto) {
        if (projeto == null || projeto.valorHoraBase() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return projeto.valorHoraBase().setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizarBonus(BigDecimal bonus) {
        if (bonus == null || bonus.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return bonus.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean profissionalAtivo(ProfissionalExternoDto profissional) {
        return profissional != null && Boolean.TRUE.equals(profissional.ativo());
    }

    private String nomeProfissional(ProfissionalExternoDto profissional, Integer usuarioId) {
        if (profissional != null && profissional.nome() != null && !profissional.nome().isBlank()) {
            return profissional.nome();
        }

        return "Usuário " + usuarioId;
    }

    private boolean tarefaConcluida(TarefaExternaDto tarefa) {
        String status = tarefa == null ? null : tarefa.status();
        if (status == null) {
            return false;
        }

        String normalizado = status.trim().toUpperCase();
        return normalizado.equals("CONCLUIDA") || normalizado.equals("CONCLUIDO");
    }

    private Integer toInteger(Long valor) {
        if (valor == null) {
            return null;
        }

        return Math.toIntExact(valor);
    }

    private String stringSeguro(String valor) {
        return valor == null ? "" : valor;
    }

    private <T> List<T> listaSegura(List<T> lista) {
        return lista == null ? List.of() : new ArrayList<>(lista);
    }

    private record PeriodoMensal(Instant inicio, Instant fim) {
    }

    private record DadosFinanceiros(
            List<ProjetoExternoDto> projetos,
            List<TarefaExternaDto> tarefas,
            List<RegistroHoraExternoDto> registros,
            List<ProfissionalExternoDto> profissionais,
            Map<Integer, ProjetoExternoDto> projetosPorId,
            Map<Integer, ProfissionalExternoDto> profissionaisPorId,
            Map<Long, List<RegistroHoraExternoDto>> registrosPorTarefaId
    ) {
    }
}