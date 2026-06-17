package com.bobbo01.supplyhub.domain.commerce.dto;

import java.math.BigDecimal;

public record CartItemView(
        Long cartItemId,
        Long productId,
        String productName,
        Integer quantity,
        Integer minOrderQty,
        BigDecimal unitPrice,
        String currencyCode,
        BigDecimal lineTotal
) {
}
