package com.bobbo01.supplyhub.domain.product.dto;

import java.math.BigDecimal;

public record ProductSummaryView(
        Long id,
        String sku,
        String productName,
        String brand,
        String categoryName,
        String imageUrl,
        BigDecimal unitPrice,
        String currencyCode,
        Integer minOrderQty
) {
}
