package com.bobbo01.supplyhub.domain.product.service;

import com.bobbo01.supplyhub.domain.category.entity.Category;
import com.bobbo01.supplyhub.domain.product.dto.ProductCatalogPageView;
import com.bobbo01.supplyhub.domain.product.dto.ProductCategorySectionView;
import com.bobbo01.supplyhub.domain.product.dto.ProductDetailView;
import com.bobbo01.supplyhub.domain.product.dto.ProductSummaryView;
import com.bobbo01.supplyhub.domain.product.entity.Product;
import com.bobbo01.supplyhub.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCatalogServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductCatalogService productCatalogService;

    @Test
    void returnsActiveProductSummaries() {
        Category category = Category.builder()
                .categoryName("Electronics")
                .categoryCode("ELEC")
                .sortOrder(1)
                .isActive(true)
                .build();
        Product product = Product.builder()
                .category(category)
                .sku("SKU-1001")
                .productName("Wireless Headset")
                .brand("SupplyTech")
                .description("Noise-cancelling headset for office calls")
                .unitPrice(new BigDecimal("1250000.00"))
                .currencyCode("KRW")
                .minOrderQty(2)
                .isActive(true)
                .build();

        when(productRepository.findAllByIsActiveTrueOrderByCategory_SortOrderAscProductNameAsc())
                .thenReturn(List.of(product));

        List<ProductSummaryView> products = productCatalogService.getActiveProducts();

        assertThat(products).hasSize(1);
        assertThat(products.getFirst().productName()).isEqualTo("Wireless Headset");
        assertThat(products.getFirst().categoryName()).isEqualTo("Electronics");
        assertThat(products.getFirst().imageUrl()).isNull();
        assertThat(products.getFirst().minOrderQty()).isEqualTo(2);
    }

    @Test
    void returnsFeaturedProductsAcrossCategoriesWithoutOverweightingOneCategory() {
        Category paper = Category.builder()
                .categoryName("Paper")
                .categoryCode("PAPER")
                .sortOrder(1)
                .isActive(true)
                .build();
        Category writing = Category.builder()
                .categoryName("Writing")
                .categoryCode("WRITING")
                .sortOrder(2)
                .isActive(true)
                .build();
        Category devices = Category.builder()
                .categoryName("Devices")
                .categoryCode("DEVICES")
                .sortOrder(3)
                .isActive(true)
                .build();

        List<Product> products = List.of(
                product(paper, "PAPER-1", "A4 Copy Paper"),
                product(paper, "PAPER-2", "Sticky Notes"),
                product(paper, "PAPER-3", "Document Folder"),
                product(writing, "WRITING-1", "Ballpoint Pen"),
                product(writing, "WRITING-2", "Highlighter"),
                product(devices, "DEVICES-1", "Barcode Scanner"),
                product(devices, "DEVICES-2", "Label Printer")
        );

        when(productRepository.findAllByIsActiveTrueOrderByCategory_SortOrderAscProductNameAsc())
                .thenReturn(products);

        List<ProductSummaryView> featuredProducts = productCatalogService.getFeaturedProducts(6);

        assertThat(featuredProducts).hasSize(6);
        assertThat(featuredProducts).extracting(ProductSummaryView::productName)
                .containsExactly(
                        "A4 Copy Paper",
                        "Ballpoint Pen",
                        "Barcode Scanner",
                        "Sticky Notes",
                        "Highlighter",
                        "Label Printer"
                );
    }

    @Test
    void returnsProductDetailWithParentCategoryName() {
        Category parentCategory = Category.builder()
                .categoryName("IT Devices")
                .categoryCode("IT")
                .sortOrder(1)
                .isActive(true)
                .build();
        Category category = Category.builder()
                .parentCategory(parentCategory)
                .categoryName("Mobile Terminals")
                .categoryCode("MOBILE")
                .sortOrder(2)
                .isActive(true)
                .build();
        Product product = Product.builder()
                .category(category)
                .sku("SKU-2001")
                .productName("Field PDA")
                .brand("FieldOps")
                .description("Rugged PDA for warehouse and field operations")
                .unitPrice(new BigDecimal("890000.00"))
                .currencyCode("KRW")
                .minOrderQty(1)
                .isActive(true)
                .build();

        when(productRepository.findByIdAndIsActiveTrue(7L)).thenReturn(Optional.of(product));

        ProductDetailView detail = productCatalogService.getActiveProduct(7L);

        assertThat(detail.productName()).isEqualTo("Field PDA");
        assertThat(detail.categoryName()).isEqualTo("Mobile Terminals");
        assertThat(detail.parentCategoryName()).isEqualTo("IT Devices");
        assertThat(detail.imageUrl()).isNull();
    }

    @Test
    void groupsActiveProductsByCategory() {
        Category paper = Category.builder()
                .categoryName("Paper")
                .categoryCode("PAPER")
                .sortOrder(1)
                .isActive(true)
                .build();
        Category writing = Category.builder()
                .categoryName("Writing")
                .categoryCode("WRITING")
                .sortOrder(2)
                .isActive(true)
                .build();
        Product firstPaperProduct = Product.builder()
                .category(paper)
                .sku("PAPER-001")
                .productName("A4 Copy Paper")
                .brand("Mirae Paper")
                .unitPrice(new BigDecimal("6900.00"))
                .currencyCode("KRW")
                .minOrderQty(5)
                .isActive(true)
                .build();
        Product secondPaperProduct = Product.builder()
                .category(paper)
                .sku("PAPER-002")
                .productName("Sticky Notes")
                .brand("MemoPlus")
                .unitPrice(new BigDecimal("1800.00"))
                .currencyCode("KRW")
                .minOrderQty(10)
                .isActive(true)
                .build();
        Product writingProduct = Product.builder()
                .category(writing)
                .sku("WRITING-001")
                .productName("Ballpoint Pen")
                .brand("UniOffice")
                .unitPrice(new BigDecimal("500.00"))
                .currencyCode("KRW")
                .minOrderQty(50)
                .isActive(true)
                .build();

        when(productRepository.findAllByIsActiveTrueOrderByCategory_SortOrderAscProductNameAsc())
                .thenReturn(List.of(firstPaperProduct, secondPaperProduct, writingProduct));

        List<ProductCategorySectionView> sections = productCatalogService.getActiveProductsByCategory();

        assertThat(sections).hasSize(2);
        assertThat(sections.get(0).categoryCode()).isEqualTo("PAPER");
        assertThat(sections.get(0).categoryName()).isEqualTo("Paper");
        assertThat(sections.get(0).productCount()).isEqualTo(2);
        assertThat(sections.get(0).products()).extracting(ProductSummaryView::productName)
                .containsExactly("A4 Copy Paper", "Sticky Notes");
        assertThat(sections.get(1).categoryCode()).isEqualTo("WRITING");
        assertThat(sections.get(1).productCount()).isEqualTo(1);
    }

    @Test
    void returnsFilteredCatalogPageForSelectedCategoryAndSearch() {
        Category paper = Category.builder()
                .categoryName("Paper")
                .categoryCode("PAPER")
                .sortOrder(1)
                .isActive(true)
                .build();
        Category writing = Category.builder()
                .categoryName("Writing")
                .categoryCode("WRITING")
                .sortOrder(2)
                .isActive(true)
                .build();
        Product paperProduct = Product.builder()
                .category(paper)
                .sku("PAPER-A4-80")
                .productName("A4 Copy Paper 80gsm")
                .brand("Mirae Paper")
                .unitPrice(new BigDecimal("6900.00"))
                .currencyCode("KRW")
                .minOrderQty(5)
                .isActive(true)
                .build();
        Product otherPaperProduct = Product.builder()
                .category(paper)
                .sku("NOTE-STICKY-76")
                .productName("Sticky Notes")
                .brand("MemoPlus")
                .unitPrice(new BigDecimal("1800.00"))
                .currencyCode("KRW")
                .minOrderQty(10)
                .isActive(true)
                .build();
        Product writingProduct = Product.builder()
                .category(writing)
                .sku("PEN-BALL-BK-07")
                .productName("Ballpoint Pen")
                .brand("UniOffice")
                .unitPrice(new BigDecimal("500.00"))
                .currencyCode("KRW")
                .minOrderQty(50)
                .isActive(true)
                .build();

        when(productRepository.findAllByIsActiveTrueOrderByCategory_SortOrderAscProductNameAsc())
                .thenReturn(List.of(paperProduct, otherPaperProduct, writingProduct));
        stubCatalogPageQueries(List.of(paperProduct, otherPaperProduct, writingProduct));

        ProductCatalogPageView catalogPage = productCatalogService.getCatalogPage("PAPER", "a4", 1);

        assertThat(catalogPage.selectedCategoryCode()).isEqualTo("PAPER");
        assertThat(catalogPage.selectedCategoryName()).isEqualTo("Paper");
        assertThat(catalogPage.searchQuery()).isEqualTo("a4");
        assertThat(catalogPage.totalItems()).isEqualTo(1);
        assertThat(catalogPage.products()).extracting(ProductSummaryView::productName)
                .containsExactly("A4 Copy Paper 80gsm");
    }

    @Test
    void fallsBackToAllCategoryWhenRequestedCategoryDoesNotExist() {
        Category paper = Category.builder()
                .categoryName("Paper")
                .categoryCode("PAPER")
                .sortOrder(1)
                .isActive(true)
                .build();
        Category writing = Category.builder()
                .categoryName("Writing")
                .categoryCode("WRITING")
                .sortOrder(2)
                .isActive(true)
                .build();

        when(productRepository.findAllByIsActiveTrueOrderByCategory_SortOrderAscProductNameAsc())
                .thenReturn(List.of(
                        product(paper, "PAPER-001", "A4 Copy Paper"),
                        product(writing, "WRITING-001", "Ballpoint Pen")
                ));
        stubCatalogPageQueries(List.of(
                product(paper, "PAPER-001", "A4 Copy Paper"),
                product(writing, "WRITING-001", "Ballpoint Pen")
        ));

        ProductCatalogPageView catalogPage = productCatalogService.getCatalogPage("UNKNOWN", "", 1);

        assertThat(catalogPage.selectedCategoryCode()).isEqualTo(ProductCatalogService.ALL_CATEGORIES);
        assertThat(catalogPage.products()).extracting(ProductSummaryView::productName)
                .containsExactly("A4 Copy Paper", "Ballpoint Pen");
    }

    @Test
    void trimsSearchQueryBeforeFilteringProducts() {
        Category paper = Category.builder()
                .categoryName("Paper")
                .categoryCode("PAPER")
                .sortOrder(1)
                .isActive(true)
                .build();

        when(productRepository.findAllByIsActiveTrueOrderByCategory_SortOrderAscProductNameAsc())
                .thenReturn(List.of(
                        product(paper, "PAPER-A4-80", "A4 Copy Paper 80gsm"),
                        product(paper, "PAPER-NOTE-1", "Sticky Notes")
                ));
        stubCatalogPageQueries(List.of(
                product(paper, "PAPER-A4-80", "A4 Copy Paper 80gsm"),
                product(paper, "PAPER-NOTE-1", "Sticky Notes")
        ));

        ProductCatalogPageView catalogPage = productCatalogService.getCatalogPage("all", "  a4  ", 1);

        assertThat(catalogPage.searchQuery()).isEqualTo("a4");
        assertThat(catalogPage.products()).extracting(ProductSummaryView::productName)
                .containsExactly("A4 Copy Paper 80gsm");
    }

    @Test
    void matchesSearchQueryCaseInsensitivelyAcrossProductNameBrandAndSku() {
        Category paper = Category.builder()
                .categoryName("Paper")
                .categoryCode("PAPER")
                .sortOrder(1)
                .isActive(true)
                .build();

        Product productNameMatch = Product.builder()
                .category(paper)
                .sku("PAPER-001")
                .productName("A4 Copy Paper")
                .brand("Mirae")
                .unitPrice(new BigDecimal("6900.00"))
                .currencyCode("KRW")
                .minOrderQty(5)
                .isActive(true)
                .build();
        Product brandMatch = Product.builder()
                .category(paper)
                .sku("PAPER-002")
                .productName("Sticky Notes")
                .brand("MemoPlus")
                .unitPrice(new BigDecimal("1800.00"))
                .currencyCode("KRW")
                .minOrderQty(10)
                .isActive(true)
                .build();
        Product skuMatch = Product.builder()
                .category(paper)
                .sku("SKU-FIELD-99")
                .productName("Document Folder")
                .brand("SupplyTech")
                .unitPrice(new BigDecimal("2500.00"))
                .currencyCode("KRW")
                .minOrderQty(3)
                .isActive(true)
                .build();

        when(productRepository.findAllByIsActiveTrueOrderByCategory_SortOrderAscProductNameAsc())
                .thenReturn(List.of(productNameMatch, brandMatch, skuMatch));
        stubCatalogPageQueries(List.of(productNameMatch, brandMatch, skuMatch));

        ProductCatalogPageView nameSearch = productCatalogService.getCatalogPage("all", "copy", 1);
        ProductCatalogPageView brandSearch = productCatalogService.getCatalogPage("all", "memoplus", 1);
        ProductCatalogPageView skuSearch = productCatalogService.getCatalogPage("all", "field-99", 1);

        assertThat(nameSearch.products()).extracting(ProductSummaryView::productName)
                .containsExactly("A4 Copy Paper");
        assertThat(brandSearch.products()).extracting(ProductSummaryView::productName)
                .containsExactly("Sticky Notes");
        assertThat(skuSearch.products()).extracting(ProductSummaryView::productName)
                .containsExactly("Document Folder");
    }

    @Test
    void clampsRequestedPageIntoValidRange() {
        Category paper = Category.builder()
                .categoryName("Paper")
                .categoryCode("PAPER")
                .sortOrder(1)
                .isActive(true)
                .build();
        List<Product> products = java.util.stream.IntStream.rangeClosed(1, 8)
                .mapToObj(index -> Product.builder()
                        .category(paper)
                        .sku("PAPER-" + index)
                        .productName("Paper Product " + index)
                        .brand("Mirae Paper")
                        .unitPrice(new BigDecimal("1000.00"))
                        .currencyCode("KRW")
                        .minOrderQty(1)
                        .isActive(true)
                        .build())
                .toList();

        when(productRepository.findAllByIsActiveTrueOrderByCategory_SortOrderAscProductNameAsc())
                .thenReturn(products);
        stubCatalogPageQueries(products);

        ProductCatalogPageView zeroPage = productCatalogService.getCatalogPage("all", "", 0);
        ProductCatalogPageView negativePage = productCatalogService.getCatalogPage("all", "", -3);
        ProductCatalogPageView oversizedPage = productCatalogService.getCatalogPage("all", "", 99);

        assertThat(zeroPage.currentPage()).isEqualTo(1);
        assertThat(negativePage.currentPage()).isEqualTo(1);
        assertThat(oversizedPage.currentPage()).isEqualTo(2);
        assertThat(oversizedPage.products()).hasSize(2);
    }

    @Test
    void returnsEmptyCatalogPageWhenNoProductsMatchSearch() {
        Category paper = Category.builder()
                .categoryName("Paper")
                .categoryCode("PAPER")
                .sortOrder(1)
                .isActive(true)
                .build();

        when(productRepository.findAllByIsActiveTrueOrderByCategory_SortOrderAscProductNameAsc())
                .thenReturn(List.of(
                        product(paper, "PAPER-001", "A4 Copy Paper"),
                        product(paper, "PAPER-002", "Sticky Notes")
                ));
        stubCatalogPageQueries(List.of(
                product(paper, "PAPER-001", "A4 Copy Paper"),
                product(paper, "PAPER-002", "Sticky Notes")
        ));

        ProductCatalogPageView catalogPage = productCatalogService.getCatalogPage("all", "scanner", 1);

        assertThat(catalogPage.totalItems()).isEqualTo(0);
        assertThat(catalogPage.totalPages()).isEqualTo(1);
        assertThat(catalogPage.currentPage()).isEqualTo(1);
        assertThat(catalogPage.products()).isEmpty();
    }

    @Test
    void keepsCategoryFilterCountsBasedOnAllActiveProductsEvenWhenSearchIsApplied() {
        Category paper = Category.builder()
                .categoryName("Paper")
                .categoryCode("PAPER")
                .sortOrder(1)
                .isActive(true)
                .build();
        Category writing = Category.builder()
                .categoryName("Writing")
                .categoryCode("WRITING")
                .sortOrder(2)
                .isActive(true)
                .build();

        when(productRepository.findAllByIsActiveTrueOrderByCategory_SortOrderAscProductNameAsc())
                .thenReturn(List.of(
                        product(paper, "PAPER-001", "A4 Copy Paper"),
                        product(paper, "PAPER-002", "Sticky Notes"),
                        product(writing, "WRITING-001", "Ballpoint Pen")
                ));
        stubCatalogPageQueries(List.of(
                product(paper, "PAPER-001", "A4 Copy Paper"),
                product(paper, "PAPER-002", "Sticky Notes"),
                product(writing, "WRITING-001", "Ballpoint Pen")
        ));

        ProductCatalogPageView catalogPage = productCatalogService.getCatalogPage("all", "a4", 1);

        assertThat(catalogPage.totalItems()).isEqualTo(1);
        assertThat(catalogPage.categories()).extracting(filter -> filter.categoryCode() + ":" + filter.productCount())
                .containsExactly("all:3", "PAPER:2", "WRITING:1");
    }

    @Test
    void preservesRepositoryOrderingAcrossCategoriesAndProductNames() {
        Category paper = Category.builder()
                .categoryName("Paper")
                .categoryCode("PAPER")
                .sortOrder(1)
                .isActive(true)
                .build();
        Category writing = Category.builder()
                .categoryName("Writing")
                .categoryCode("WRITING")
                .sortOrder(2)
                .isActive(true)
                .build();

        Product alphaPaper = product(paper, "PAPER-002", "Alpha Paper");
        Product betaPaper = product(paper, "PAPER-003", "Beta Paper");
        Product alphaPen = product(writing, "WRITING-001", "Alpha Pen");

        when(productRepository.findAllByIsActiveTrueOrderByCategory_SortOrderAscProductNameAsc())
                .thenReturn(List.of(alphaPaper, betaPaper, alphaPen));
        stubCatalogPageQueries(List.of(alphaPaper, betaPaper, alphaPen));

        ProductCatalogPageView catalogPage = productCatalogService.getCatalogPage("all", "", 1);

        assertThat(catalogPage.products()).extracting(ProductSummaryView::productName)
                .containsExactly("Alpha Paper", "Beta Paper", "Alpha Pen");
    }

    @Test
    void keepsLastPageAsPartialPage() {
        Category paper = Category.builder()
                .categoryName("Paper")
                .categoryCode("PAPER")
                .sortOrder(1)
                .isActive(true)
                .build();
        List<Product> products = java.util.stream.IntStream.rangeClosed(1, 7)
                .mapToObj(index -> Product.builder()
                        .category(paper)
                        .sku("PAPER-" + index)
                        .productName("Paper Product " + index)
                        .brand("Mirae Paper")
                        .unitPrice(new BigDecimal("1000.00"))
                        .currencyCode("KRW")
                        .minOrderQty(1)
                        .isActive(true)
                        .build())
                .toList();

        when(productRepository.findAllByIsActiveTrueOrderByCategory_SortOrderAscProductNameAsc())
                .thenReturn(products);
        stubCatalogPageQueries(products);

        ProductCatalogPageView lastPage = productCatalogService.getCatalogPage("all", "", 2);

        assertThat(lastPage.currentPage()).isEqualTo(2);
        assertThat(lastPage.totalPages()).isEqualTo(2);
        assertThat(lastPage.products()).hasSize(1);
        assertThat(lastPage.products().getFirst().productName()).isEqualTo("Paper Product 7");
    }

    @Test
    void paginatesCatalogPageBySixItems() {
        Category paper = Category.builder()
                .categoryName("Paper")
                .categoryCode("PAPER")
                .sortOrder(1)
                .isActive(true)
                .build();
        List<Product> products = java.util.stream.IntStream.rangeClosed(1, 8)
                .mapToObj(index -> Product.builder()
                        .category(paper)
                        .sku("PAPER-" + index)
                        .productName("Paper Product " + index)
                        .brand("Mirae Paper")
                        .unitPrice(new BigDecimal("1000.00"))
                        .currencyCode("KRW")
                        .minOrderQty(1)
                        .isActive(true)
                        .build())
                .toList();

        when(productRepository.findAllByIsActiveTrueOrderByCategory_SortOrderAscProductNameAsc())
                .thenReturn(products);
        stubCatalogPageQueries(products);

        ProductCatalogPageView firstPage = productCatalogService.getCatalogPage("all", "", 1);
        ProductCatalogPageView secondPage = productCatalogService.getCatalogPage("all", "", 2);

        assertThat(firstPage.pageSize()).isEqualTo(6);
        assertThat(firstPage.totalItems()).isEqualTo(8);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.products()).hasSize(6);
        assertThat(secondPage.products()).hasSize(2);
        assertThat(secondPage.currentPage()).isEqualTo(2);
    }

    @Test
    void limitsVisiblePageNumbersToFiveAndMovesByPageGroup() {
        Category paper = Category.builder()
                .categoryName("Paper")
                .categoryCode("PAPER")
                .sortOrder(1)
                .isActive(true)
                .build();
        List<Product> products = java.util.stream.IntStream.rangeClosed(1, 32)
                .mapToObj(index -> Product.builder()
                        .category(paper)
                        .sku("PAPER-" + index)
                        .productName("Paper Product " + index)
                        .brand("Mirae Paper")
                        .unitPrice(new BigDecimal("1000.00"))
                        .currencyCode("KRW")
                        .minOrderQty(1)
                        .isActive(true)
                        .build())
                .toList();

        when(productRepository.findAllByIsActiveTrueOrderByCategory_SortOrderAscProductNameAsc())
                .thenReturn(products);
        stubCatalogPageQueries(products);

        ProductCatalogPageView firstWindow = productCatalogService.getCatalogPage("all", "", 1);
        ProductCatalogPageView secondWindow = productCatalogService.getCatalogPage("all", "", 6);

        assertThat(firstWindow.totalPages()).isEqualTo(6);
        assertThat(firstWindow.pageNumbers()).containsExactly(1, 2, 3, 4, 5);
        assertThat(firstWindow.hasPreviousPageGroup()).isFalse();
        assertThat(firstWindow.hasNextPageGroup()).isTrue();
        assertThat(firstWindow.nextPageGroupPage()).isEqualTo(6);

        assertThat(secondWindow.pageNumbers()).containsExactly(6);
        assertThat(secondWindow.hasPreviousPageGroup()).isTrue();
        assertThat(secondWindow.previousPageGroupPage()).isEqualTo(1);
        assertThat(secondWindow.hasNextPageGroup()).isFalse();
    }

    @Test
    void throwsNotFoundForMissingActiveProduct() {
        when(productRepository.findByIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productCatalogService.getActiveProduct(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    private Product product(Category category, String sku, String productName) {
        return Product.builder()
                .category(category)
                .sku(sku)
                .productName(productName)
                .brand("SupplyTech")
                .unitPrice(new BigDecimal("1000.00"))
                .currencyCode("KRW")
                .minOrderQty(1)
                .isActive(true)
                .build();
    }

    private void stubCatalogPageQueries(List<Product> activeProducts) {
        lenient().when(productRepository.existsByIsActiveTrueAndCategory_CategoryCode(anyString()))
                .thenAnswer(invocation -> activeProducts.stream()
                        .anyMatch(product -> product.getCategory().getCategoryCode().equals(invocation.getArgument(0))));
        lenient().when(productRepository.countCatalogProducts(anyString(), anyString()))
                .thenAnswer(invocation -> (long) filterProducts(
                        activeProducts,
                        invocation.getArgument(0),
                        invocation.getArgument(1)
                ).size());
        lenient().when(productRepository.findCatalogPage(anyString(), anyString(), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    List<Product> filteredProducts = filterProducts(
                            activeProducts,
                            invocation.getArgument(0),
                            invocation.getArgument(1)
                    );
                    Pageable pageable = invocation.getArgument(2);
                    int fromIndex = (int) Math.min(pageable.getOffset(), filteredProducts.size());
                    int toIndex = Math.min(fromIndex + pageable.getPageSize(), filteredProducts.size());
                    List<Product> pageContent = filteredProducts.subList(fromIndex, toIndex);
                    return new PageImpl<>(pageContent, pageable, filteredProducts.size());
                });
    }

    private List<Product> filterProducts(List<Product> activeProducts, String categoryCode, String normalizedQuery) {
        return activeProducts.stream()
                .filter(product -> ProductCatalogService.ALL_CATEGORIES.equals(categoryCode)
                        || product.getCategory().getCategoryCode().equals(categoryCode))
                .filter(product -> matchesSearch(product, normalizedQuery))
                .toList();
    }

    private boolean matchesSearch(Product product, String normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            return true;
        }

        return containsIgnoreCase(product.getProductName(), normalizedQuery)
                || containsIgnoreCase(product.getBrand(), normalizedQuery)
                || containsIgnoreCase(product.getSku(), normalizedQuery);
    }

    private boolean containsIgnoreCase(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }
}
