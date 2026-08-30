package com.fantone.app_saos.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RechargeCardRequestDto(
        @NotNull(message = "L'importo è obbligatorio")
        @DecimalMin(value = "0.01", message = "L'importo deve essere superiore a zero")
        BigDecimal amount
) {}