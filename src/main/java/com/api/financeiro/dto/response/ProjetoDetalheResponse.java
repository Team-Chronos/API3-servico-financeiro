package com.api.financeiro.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ProjetoDetalheResponse(
        Integer projetoId,
        String nomeProjeto,
        String tipoProjeto,
        BigDecimal totalHoras,
        BigDecimal custoTotal,
        BigDecimal valorHoraProjeto,
        Integer totalProfissionais,
        List<ProjetoProfissionalResponse> profissionais
) {
}