package com.api.financeiro.client.dto;

import java.math.BigDecimal;

public record ProjetoVinculadoExternoDto(
        Integer projetoId,
        String nomeProjeto,
        String codigoProjeto,
        BigDecimal valorHora
) {
}
