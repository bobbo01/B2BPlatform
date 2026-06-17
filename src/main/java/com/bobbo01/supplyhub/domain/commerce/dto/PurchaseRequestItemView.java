package com.bobbo01.supplyhub.domain.commerce.dto;

import java.math.BigDecimal;

public record PurchaseRequestItemView(
        Long purchaseRequestItemId,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        String currencyCode,
        BigDecimal lineTotal
) {
}
