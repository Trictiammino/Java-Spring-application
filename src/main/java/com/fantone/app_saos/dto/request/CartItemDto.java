package com.fantone.app_saos.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemDto(
        @NotNull
        Long id,

        @Min(value = 1, message = "La quantità deve essere almeno 1")
        int quantity
) {}
