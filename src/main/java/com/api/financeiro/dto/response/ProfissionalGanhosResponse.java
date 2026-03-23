package com.api.financeiro.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ProfissionalGanhosResponse(
        Integer usuarioId,
        String usuarioNome,
        List<ProjetoProfissionalResponse> projetos,
        BigDecimal totalSemBonus,
        BigDecimal bonusAplicado,
        BigDecimal totalComBonus
) {
}