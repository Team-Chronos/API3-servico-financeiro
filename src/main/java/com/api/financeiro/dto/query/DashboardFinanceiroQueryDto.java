package com.api.financeiro.dto.query;

import java.math.BigDecimal;

public record DashboardFinanceiroQueryDto(
        BigDecimal totalHoras,
        BigDecimal custoTotal,
        Long totalProjetos,
        Long tarefasConcluidas,
        Long projetosConcluidos,
        Long totalDesenvolvedores
) {
}