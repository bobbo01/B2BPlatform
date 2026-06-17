package com.bobbo01.supplyhub.domain.product.service;

import com.bobbo01.supplyhub.domain.category.entity.Category;
import com.bobbo01.supplyhub.domain.product.dto.ProductCatalogPageView;
import com.bobbo01.supplyhub.domain.product.dto.ProductCategoryFilterView;
import com.bobbo01.supplyhub.domain.product.dto.ProductCategorySectionView;
import com.bobbo01.supplyhub.domain.product.dto.ProductDetailView;
import com.bobbo01.supplyhub.domain.product.dto.ProductSummaryView;
import com.bobbo01.supplyhub.domain.product.entity.Product;
import com.bobbo01.supplyhub.domain.product.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ProductCatalogService {

    public static final String ALL_CATEGORIES = "all";
    public static final int PRODUCT_PAGE_SIZE = 6;
    public static final int PAGE_NUMBER_WINDOW_SIZE = 5;
    private static final String PRODUCT_IMAGE_BASE_PATH = "/images/products/";

    private final ProductRepository productRepository;

    public ProductCatalogService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductSummaryView> getActiveProducts() {
        return productRepository.findAllByIsActiveTrueOrderByCategory_SortOrderAscProductNameAsc().stream()
                .map(this::toSummaryView)
                .toList();
    }

    public List<ProductSummaryView> getFeaturedProducts(int limit) {
        if (limit <= 0) {
            return List.of();
        }

        List<ProductCategorySectionView> sections = getActiveProductsByCategory();
        List<ProductSummaryView> featuredProducts = new ArrayList<>(Math.min(limit, PRODUCT_PAGE_SIZE));
        int productIndex = 0;

        while (featuredProducts.size() < limit) {
            boolean addedInThisRound = false;

            for (ProductCategorySectionView section : sections) {
                if (featuredProducts.size() >= limit) {
                    break;
                }
                if (section.products().size() <= productIndex) {
                    continue;
                }

                featuredProducts.add(section.products().get(productIndex));
                addedInThisRound = true;
            }

            if (!addedInThisRound) {
                break;
            }

            productIndex++;
        }

        return List.copyOf(featuredProducts);
    }

    public List<ProductCategorySectionView> getActiveProductsByCategory() {
        Map<String, CategorySectionAccumulator> sections = new LinkedHashMap<>();

        for (Product product : productRepository.findAllByIsActiveTrueOrderByCategory_SortOrderAscProductNameAsc()) {
            Category category = product.getCategory();
            ProductSummaryView summaryView = toSummaryView(product);
            sections.computeIfAbsent(
                    category.getCategoryCode(),
                    categoryCode -> new CategorySectionAccumulator(categoryCode, category.getCategoryName(), new ArrayList<>())
            ).products().add(summaryView);
        }

        return sections.values().stream()
                .map(section -> new ProductCategorySectionView(
                        section.categoryCode(),
                        section.categoryName(),
                        section.products().size(),
                        List.copyOf(section.products())
                ))
                .toList();
    }

    public ProductCatalogPageView getCatalogPage(String requestedCategoryCode, String requestedQuery, Integer requestedPage) {
        List<Product> activeProducts = productRepository.findAllByIsActiveTrueOrderByCategory_SortOrderAscProductNameAsc();
        String selectedCategoryCode = normalizeCategoryCode(requestedCategoryCode);
        String displayQuery = normalizeDisplayQuery(requestedQuery);
        String normalizedQuery = displayQuery.toLowerCase(Locale.ROOT);
        if (!ALL_CATEGORIES.equals(selectedCategoryCode)
                && !productRepository.existsByIsActiveTrueAndCategory_CategoryCode(selectedCategoryCode)) {
            selectedCategoryCode = ALL_CATEGORIES;
        }
        final String effectiveCategoryCode = selectedCategoryCode;

        List<ProductCategoryFilterView> categories = buildCategoryFilters(activeProducts, effectiveCategoryCode);
        int totalItems = Math.toIntExact(productRepository.countCatalogProducts(effectiveCategoryCode, normalizedQuery));
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / PRODUCT_PAGE_SIZE));
        int currentPage = clampPage(requestedPage, totalPages);
        List<ProductSummaryView> pagedProducts = productRepository.findCatalogPage(
                        effectiveCategoryCode,
                        normalizedQuery,
                        PageRequest.of(currentPage - 1, PRODUCT_PAGE_SIZE)
                ).getContent().stream()
                .map(this::toSummaryView)
                .toList();
        PageWindow pageWindow = buildPageWindow(currentPage, totalPages);

        return new ProductCatalogPageView(
                effectiveCategoryCode,
                resolveSelectedCategoryName(categories, effectiveCategoryCode),
                displayQuery,
                currentPage,
                PRODUCT_PAGE_SIZE,
                totalItems,
                totalPages,
                pageWindow.pageNumbers(),
                currentPage > 1,
                currentPage < totalPages,
                Math.max(1, currentPage - 1),
                Math.min(totalPages, currentPage + 1),
                pageWindow.hasPreviousPageGroup(),
                pageWindow.hasNextPageGroup(),
                pageWindow.previousPageGroupPage(),
                pageWindow.nextPageGroupPage(),
                categories,
                pagedProducts
        );
    }

    public ProductDetailView getActiveProduct(Long productId) {
        Product product = productRepository.findByIdAndIsActiveTrue(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
        return toDetailView(product);
    }

    private ProductSummaryView toSummaryView(Product product) {
        return new ProductSummaryView(
                product.getId(),
                product.getSku(),
                product.getProductName(),
                product.getBrand(),
                product.getCategory().getCategoryName(),
                resolveProductImageUrl(product.getImageUrl()),
                product.getUnitPrice(),
                product.getCurrencyCode(),
                product.getMinOrderQty()
        );
    }

    private ProductDetailView toDetailView(Product product) {
        Category parentCategory = product.getCategory().getParentCategory();
        return new ProductDetailView(
                product.getId(),
                product.getSku(),
                product.getProductName(),
                product.getBrand(),
                product.getDescription(),
                product.getCategory().getCategoryName(),
                parentCategory != null ? parentCategory.getCategoryName() : null,
                resolveProductImageUrl(product.getImageUrl()),
                product.getUnitPrice(),
                product.getCurrencyCode(),
                product.getMinOrderQty()
        );
    }

    private String resolveProductImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }

        if (imageUrl.startsWith("/")) {
            return imageUrl;
        }

        return PRODUCT_IMAGE_BASE_PATH + imageUrl;
    }

    private List<ProductCategoryFilterView> buildCategoryFilters(List<Product> activeProducts, String selectedCategoryCode) {
        Map<String, CategoryCounter> counters = new LinkedHashMap<>();

        for (Product product : activeProducts) {
            Category category = product.getCategory();
            counters.computeIfAbsent(
                    category.getCategoryCode(),
                    ignored -> new CategoryCounter(category.getCategoryCode(), category.getCategoryName(), 0)
            ).increment();
        }

        List<ProductCategoryFilterView> categories = new ArrayList<>();
        categories.add(new ProductCategoryFilterView(
                ALL_CATEGORIES,
                "전체",
                activeProducts.size(),
                ALL_CATEGORIES.equals(selectedCategoryCode)
        ));

        for (CategoryCounter counter : counters.values()) {
            categories.add(new ProductCategoryFilterView(
                    counter.categoryCode(),
                    counter.categoryName(),
                    counter.productCount(),
                    counter.categoryCode().equals(selectedCategoryCode)
            ));
        }

        return List.copyOf(categories);
    }

    private String normalizeCategoryCode(String requestedCategoryCode) {
        if (requestedCategoryCode == null || requestedCategoryCode.isBlank()) {
            return ALL_CATEGORIES;
        }
        return requestedCategoryCode.trim();
    }

    private String normalizeDisplayQuery(String requestedQuery) {
        if (requestedQuery == null) {
            return "";
        }
        return requestedQuery.trim();
    }

    private int clampPage(Integer requestedPage, int totalPages) {
        int page = requestedPage == null ? 1 : requestedPage;
        return Math.max(1, Math.min(page, totalPages));
    }

    private PageWindow buildPageWindow(int currentPage, int totalPages) {
        int windowIndex = (currentPage - 1) / PAGE_NUMBER_WINDOW_SIZE;
        int startPage = windowIndex * PAGE_NUMBER_WINDOW_SIZE + 1;
        int endPage = Math.min(startPage + PAGE_NUMBER_WINDOW_SIZE - 1, totalPages);

        List<Integer> pageNumbers = new ArrayList<>(endPage - startPage + 1);
        for (int page = startPage; page <= endPage; page++) {
            pageNumbers.add(page);
        }

        boolean hasPreviousPageGroup = startPage > 1;
        boolean hasNextPageGroup = endPage < totalPages;

        return new PageWindow(
                List.copyOf(pageNumbers),
                hasPreviousPageGroup,
                hasNextPageGroup,
                Math.max(1, startPage - PAGE_NUMBER_WINDOW_SIZE),
                Math.min(totalPages, endPage + 1)
        );
    }

    private String resolveSelectedCategoryName(List<ProductCategoryFilterView> categories, String selectedCategoryCode) {
        return categories.stream()
                .filter(category -> category.categoryCode().equals(selectedCategoryCode))
                .map(ProductCategoryFilterView::categoryName)
                .findFirst()
                .orElse("전체");
    }

    private record CategorySectionAccumulator(
            String categoryCode,
            String categoryName,
            List<ProductSummaryView> products
    ) {
    }

    private static final class CategoryCounter {

        private final String categoryCode;
        private final String categoryName;
        private int productCount;

        private CategoryCounter(String categoryCode, String categoryName, int productCount) {
            this.categoryCode = categoryCode;
            this.categoryName = categoryName;
            this.productCount = productCount;
        }

        private void increment() {
            productCount++;
        }

        private String categoryCode() {
            return categoryCode;
        }

        private String categoryName() {
            return categoryName;
        }

        private int productCount() {
            return productCount;
        }
    }

    private record PageWindow(
            List<Integer> pageNumbers,
            boolean hasPreviousPageGroup,
            boolean hasNextPageGroup,
            int previousPageGroupPage,
            int nextPageGroupPage
    ) {
    }
}
