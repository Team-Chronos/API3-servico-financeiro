package com.api.financeiro.dto.response;

import java.math.BigDecimal;

public record ProjetoProfissionalResponse(
        Integer projetoId,
        String nomeProjeto,
        BigDecimal horasTrabalhadas,
        BigDecimal valorHoraProjeto,
        BigDecimal valorBaseCalculado
) {
}