package com.fantone.app_saos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
        @NotBlank
        @Size(min = 5, max = 30)
        String email,

        @NotBlank
        @Size(min = 8, max = 200)
        String password
) {}
