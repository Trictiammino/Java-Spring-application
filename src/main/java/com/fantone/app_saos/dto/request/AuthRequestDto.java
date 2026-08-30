package com.fantone.app_saos.dto.request;

import jakarta.validation.constraints.*;

public record AuthRequestDto(

        @NotBlank
        @Size(min = 5, max = 30)
        String username,

        @NotBlank
        @Size(min = 8, max = 200)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=(?:.*\\d){8,})(?=.*[@#$%^&*!?])[A-Za-z\\d@#$%^&*!?]+$",
                message = "La password deve contenere almeno una maiuscola, una minuscola, otto numeri e un carattere speciale"
        )
        String password,

        @NotBlank
        String name,

        @NotBlank
        String lastname,

        @Email
        @NotBlank
        String email,

        @NotBlank
        String address,

        @Min(14)
        @Max(120)
        Integer age
) {}
