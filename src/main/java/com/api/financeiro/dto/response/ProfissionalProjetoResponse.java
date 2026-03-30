package com.api.financeiro.dto.response;

import java.math.BigDecimal;

public record ProfissionalProjetoResponse(
        Integer usuarioId,
        String usuarioNome,
        BigDecimal horasTrabalhadas,
        BigDecimal valorHoraProjeto,
        BigDecimal valorBaseCalculado
) {
}