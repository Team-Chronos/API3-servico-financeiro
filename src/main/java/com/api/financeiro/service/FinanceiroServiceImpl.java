package com.api.financeiro.service;

import com.api.financeiro.dto.query.DashboardFinanceiroQueryDto;
import com.api.financeiro.dto.query.ProfissionalProjetoQueryDto;
import com.api.financeiro.dto.query.ProjetoFinanceiroQueryDto;
import com.api.financeiro.dto.query.ProjetoProfissionalQueryDto;
import com.api.financeiro.dto.query.UsuarioAtivoDto;
import com.api.financeiro.dto.response.DashboardFinanceiroResponse;
import com.api.financeiro.dto.response.ProfissionalGanhosResponse;
import com.api.financeiro.dto.response.ProfissionalProjetoResponse;
import com.api.financeiro.dto.response.ProjetoDetalheResponse;
import com.api.financeiro.dto.response.ProjetoFinanceiroResponse;
import com.api.financeiro.dto.response.ProjetoProfissionalResponse;
import com.api.financeiro.exception.RecursoNaoEncontradoException;
import com.api.financeiro.repository.FinanceiroQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class FinanceiroServiceImpl implements FinanceiroService {

    private final FinanceiroQueryRepository financeiroQueryRepository;

    public FinanceiroServiceImpl(FinanceiroQueryRepository financeiroQueryRepository) {
        this.financeiroQueryRepository = financeiroQueryRepository;
    }

    @Override
    public List<ProjetoFinanceiroResponse> listarProjetosFinanceiro() {
        return financeiroQueryRepository.listarProjetosFinanceiro()
                .stream()
                .map(this::toProjetoFinanceiroResponse)
                .toList();
    }

    @Override
    public ProjetoDetalheResponse detalharProjeto(Integer projetoId) {
        List<ProjetoProfissionalQueryDto> rows =
                financeiroQueryRepository.listarProfissionaisDoProjeto(projetoId);

        if (rows.isEmpty()) {
            throw new RecursoNaoEncontradoException("Nenhum dado encontrado para o projeto id=" + projetoId);
        }

        ProjetoProfissionalQueryDto first = rows.get(0);

        List<ProfissionalProjetoResponse> profissionais = rows.stream()
                .map(this::toProfissionalProjetoResponse)
                .toList();

        BigDecimal totalHoras = profissionais.stream()
                .map(ProfissionalProjetoResponse::horasTrabalhadas)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal custoTotal = profissionais.stream()
                .map(ProfissionalProjetoResponse::valorBaseCalculado)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal valorHoraProjeto = first.valorHoraProjeto().setScale(2, RoundingMode.HALF_UP);

        return new ProjetoDetalheResponse(
                first.projetoId(),
                first.nomeProjeto(),
                first.tipoProjeto(),
                totalHoras,
                custoTotal,
                valorHoraProjeto,
                profissionais.size(),
                profissionais
        );
    }

    @Override
    public ProfissionalGanhosResponse detalharGanhosProfissional(Integer usuarioId, BigDecimal bonus) {
        BigDecimal bonusSeguro = normalizarBonus(bonus);

        List<ProfissionalProjetoQueryDto> rows =
                financeiroQueryRepository.listarProjetosDoProfissional(usuarioId);

        if (rows.isEmpty()) {
            throw new RecursoNaoEncontradoException("Nenhum apontamento encontrado para o usuário id=" + usuarioId);
        }

        String usuarioNome = rows.get(0).usuarioNome();

        List<ProjetoProfissionalResponse> projetos = rows.stream()
                .map(this::toProjetoProfissionalResponse)
                .toList();

        BigDecimal totalSemBonus = projetos.stream()
                .map(ProjetoProfissionalResponse::valorBaseCalculado)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalComBonus = totalSemBonus
                .add(bonusSeguro)
                .setScale(2, RoundingMode.HALF_UP);

        return new ProfissionalGanhosResponse(
                usuarioId,
                usuarioNome,
                projetos,
                totalSemBonus,
                bonusSeguro,
                totalComBonus
        );
    }

    @Override
    public List<ProfissionalGanhosResponse> listarTodosProfissionais() {
        List<UsuarioAtivoDto> usuarios = financeiroQueryRepository.listarUsuariosAtivosComApontamento();

        return usuarios.stream()
                .map(usuario -> detalharGanhosProfissional(usuario.usuarioId(), BigDecimal.ZERO))
                .toList();
    }

    @Override
    public DashboardFinanceiroResponse obterDadosDashboard() {
        DashboardFinanceiroQueryDto dto = financeiroQueryRepository.obterDashboard();

        return new DashboardFinanceiroResponse(
                dto.totalHoras().setScale(2, RoundingMode.HALF_UP),
                dto.custoTotal().setScale(2, RoundingMode.HALF_UP),
                dto.totalProjetos(),
                dto.tarefasConcluidas(),
                dto.projetosConcluidos(),
                dto.totalDesenvolvedores()
        );
    }

    private ProjetoFinanceiroResponse toProjetoFinanceiroResponse(ProjetoFinanceiroQueryDto dto) {
        return new ProjetoFinanceiroResponse(
                dto.projetoId(),
                dto.nomeProjeto(),
                dto.tipoProjeto(),
                dto.totalHoras().setScale(2, RoundingMode.HALF_UP),
                dto.custoTotal().setScale(2, RoundingMode.HALF_UP)
        );
    }

    private ProjetoProfissionalResponse toProjetoProfissionalResponse(ProfissionalProjetoQueryDto dto) {
        BigDecimal valorBaseCalculado = dto.valorHoraProjeto()
                .multiply(dto.horasTrabalhadas())
                .setScale(2, RoundingMode.HALF_UP);

        return new ProjetoProfissionalResponse(
                dto.projetoId(),
                dto.nomeProjeto(),
                dto.horasTrabalhadas().setScale(2, RoundingMode.HALF_UP),
                dto.valorHoraProjeto().setScale(2, RoundingMode.HALF_UP),
                valorBaseCalculado
        );
    }

    private ProfissionalProjetoResponse toProfissionalProjetoResponse(ProjetoProfissionalQueryDto dto) {
        BigDecimal valorBaseCalculado = dto.valorHoraProjeto()
                .multiply(dto.horasTrabalhadas())
                .setScale(2, RoundingMode.HALF_UP);

        return new ProfissionalProjetoResponse(
                dto.usuarioId(),
                dto.usuarioNome(),
                dto.horasTrabalhadas().setScale(2, RoundingMode.HALF_UP),
                dto.valorHoraProjeto().setScale(2, RoundingMode.HALF_UP),
                valorBaseCalculado
        );
    }

    private BigDecimal normalizarBonus(BigDecimal bonus) {
        if (bonus == null || bonus.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return bonus.setScale(2, RoundingMode.HALF_UP);
    }
}