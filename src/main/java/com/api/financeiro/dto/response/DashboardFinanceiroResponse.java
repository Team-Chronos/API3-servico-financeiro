package com.api.financeiro.dto.response;

import java.math.BigDecimal;

public record DashboardFinanceiroResponse(
        BigDecimal totalHoras,
        BigDecimal custoTotal,
        Long totalProjetos,
        Long tarefasConcluidas,
        Long projetosConcluidos,
        Long totalDesenvolvedores
) {
}