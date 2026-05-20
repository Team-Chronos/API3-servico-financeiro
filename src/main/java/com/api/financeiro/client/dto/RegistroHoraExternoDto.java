package com.api.financeiro.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;

public record RegistroHoraExternoDto(
        Long id,
        @JsonAlias({"tarefa_id", "tarefaId"}) Long tarefaId,
        @JsonAlias({"data_inicio", "dataInicio"}) Instant dataInicio,
        @JsonAlias({"data_fim", "dataFim"}) Instant dataFim,
        Long tempoMinutos
) {
}
