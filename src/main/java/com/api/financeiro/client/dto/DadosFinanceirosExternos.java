package com.api.financeiro.client.dto;

import java.util.List;

public record DadosFinanceirosExternos(
        List<ProjetoExternoDto> projetos,
        List<TarefaExternaDto> tarefas,
        List<RegistroHoraExternoDto> registros,
        List<ProfissionalExternoDto> profissionais
) {
}
