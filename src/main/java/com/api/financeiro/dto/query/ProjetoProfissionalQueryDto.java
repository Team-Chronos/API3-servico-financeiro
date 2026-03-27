package com.api.financeiro.dto.query;

import java.math.BigDecimal;

public record ProjetoProfissionalQueryDto(
        Integer projetoId,
        String nomeProjeto,
        String tipoProjeto,
        Integer usuarioId,
        String usuarioNome,
        BigDecimal horasTrabalhadas,
        BigDecimal valorHoraProjeto
) {
}