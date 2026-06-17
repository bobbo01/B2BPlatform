package com.bobbo01.supplyhub.domain.product.dto;

import java.util.List;

public record ProductCatalogPageView(
        String selectedCategoryCode,
        String selectedCategoryName,
        String searchQuery,
        int currentPage,
        int pageSize,
        int totalItems,
        int totalPages,
        List<Integer> pageNumbers,
        boolean hasPrevious,
        boolean hasNext,
        int previousPage,
        int nextPage,
        boolean hasPreviousPageGroup,
        boolean hasNextPageGroup,
        int previousPageGroupPage,
        int nextPageGroupPage,
        List<ProductCategoryFilterView> categories,
        List<ProductSummaryView> products
) {
}
