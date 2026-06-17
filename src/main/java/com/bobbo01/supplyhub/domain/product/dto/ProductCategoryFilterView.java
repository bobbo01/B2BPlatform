package com.bobbo01.supplyhub.domain.product.dto;

public record ProductCategoryFilterView(
        String categoryCode,
        String categoryName,
        int productCount,
        boolean selected
) {
}
