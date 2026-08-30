package com.fantone.app_saos.dto.response;

import java.time.LocalDateTime;

public class PurchaseHistoryDto {

    private String productName;

    private Double price;

    private Integer quantity;

    private Double total;

    private LocalDateTime purchasedAt;

    public PurchaseHistoryDto(
            String productName,
            Double price,
            Integer quantity,
            Double total,
            LocalDateTime purchasedAt
    ) {
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.total = total;
        this.purchasedAt = purchasedAt;
    }

    public String getProductName() {
        return productName;
    }

    public Double getPrice() {
        return price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Double getTotal() {
        return total;
    }

    public LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }
}