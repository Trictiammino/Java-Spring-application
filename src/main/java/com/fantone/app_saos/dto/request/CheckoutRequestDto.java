package com.fantone.app_saos.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CheckoutRequestDto(
        @NotNull @NotEmpty(message = "Il carrello è vuoto")
        List<@Valid CartItemDto> items
) {}