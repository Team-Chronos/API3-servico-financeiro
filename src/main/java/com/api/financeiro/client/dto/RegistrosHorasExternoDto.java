package com.api.financeiro.client.dto;

import java.util.List;

public record RegistrosHorasExternoDto(
        List<RegistroHoraExternoDto> registros,
        Long tempoMinutos
) {
}
