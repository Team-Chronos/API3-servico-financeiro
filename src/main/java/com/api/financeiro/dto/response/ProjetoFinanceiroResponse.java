package com.api.financeiro.dto.response;

import java.math.BigDecimal;

public record ProjetoFinanceiroResponse(
        Integer projetoId,
        String nomeProjeto,
        String tipoProjeto,
        BigDecimal totalHoras,
        BigDecimal custoTotal
) {
}