package com.api.financeiro.dto.query;

import java.math.BigDecimal;

public record ProfissionalProjetoQueryDt(
        Integer usuarioId,
        String usuarioNome,
        Integer projetoId,
        String nomeProjeto,
        BigDecimal horasTrabalhadas,
        BigDecimal valorHoraProjeto
) {
}