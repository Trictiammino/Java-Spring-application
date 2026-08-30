package com.fantone.app_saos.dto.response;

public record ProductInfoDto(
        Long id,
        String name,
        Double price,
        Integer stockQuantity) {}