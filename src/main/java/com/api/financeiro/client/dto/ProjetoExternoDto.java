package com.api.financeiro.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjetoExternoDto(
        Integer id,
        String nome,
        String codigo,
        String tipoProjeto,
        BigDecimal valorHoraBase,
        BigDecimal horasContratadas,
        BigDecimal valorTotal,
        LocalDate dataInicio,
        LocalDate dataFim,
        Integer responsavelId
) {
}
