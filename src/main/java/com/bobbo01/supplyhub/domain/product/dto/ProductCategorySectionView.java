package com.bobbo01.supplyhub.domain.product.dto;

import java.util.List;

public record ProductCategorySectionView(
        String categoryCode,
        String categoryName,
        int productCount,
        List<ProductSummaryView> products
) {
}
