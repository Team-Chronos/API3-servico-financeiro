package com.api.financeiro.dto.query;

import java.math.BigDecimal;

public record ProjetoFinanceiroQueryDto(
        Integer projetoId,
        String nomeProjeto,
        String tipoProjeto,
        BigDecimal totalHoras,
        BigDecimal custoTotal
) {
}