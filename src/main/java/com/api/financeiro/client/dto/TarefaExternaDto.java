package com.api.financeiro.client.dto;

public record TarefaExternaDto(
        Long id,
        String titulo,
        String descricao,
        Long responsavelId,
        Long tempoMaximoMinutos,
        String status,
        Long projetoId,
        Long tipoTarefaId,
        Long itemId
) {
}
