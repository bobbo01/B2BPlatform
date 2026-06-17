package com.bobbo01.supplyhub.domain.product.dto;

import java.math.BigDecimal;

public record ProductDetailView(
        Long id,
        String sku,
        String productName,
        String brand,
        String description,
        String categoryName,
        String parentCategoryName,
        String imageUrl,
        BigDecimal unitPrice,
        String currencyCode,
        Integer minOrderQty
) {
}
