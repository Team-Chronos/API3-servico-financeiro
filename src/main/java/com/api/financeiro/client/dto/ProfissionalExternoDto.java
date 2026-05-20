package com.api.financeiro.client.dto;

import java.util.List;

public record ProfissionalExternoDto(
        Integer id,
        String nome,
        String email,
        Boolean ativo,
        Integer cargoId,
        List<ProjetoVinculadoExternoDto> projetos
) {
}
