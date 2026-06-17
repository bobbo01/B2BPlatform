package com.bobbo01.supplyhub.domain.product.controller;

import com.bobbo01.supplyhub.domain.product.dto.ProductCatalogPageView;
import com.bobbo01.supplyhub.domain.product.dto.ProductCategoryFilterView;
import com.bobbo01.supplyhub.domain.product.dto.ProductDetailView;
import com.bobbo01.supplyhub.domain.product.dto.ProductSummaryView;
import com.bobbo01.supplyhub.domain.product.service.ProductCatalogService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductCatalogService productCatalogService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private ProductController productController;

    @Test
    void rendersProductListPage() {
        ProductCatalogPageView catalogPage = new ProductCatalogPageView(
                "all",
                "전체",
                "",
                1,
                6,
                1,
                1,
                List.of(1),
                false,
                false,
                1,
                1,
                false,
                false,
                1,
                1,
                List.of(new ProductCategoryFilterView("all", "전체", 1, true)),
                List.of(new ProductSummaryView(
                        1L,
                        "SKU-1",
                        "Wireless Mouse",
                        "SupplyTech",
                        "Stationery",
                        "/images/products/sku-1.png",
                        new BigDecimal("320000.00"),
                        "KRW",
                        3
                ))
        );
        when(productCatalogService.getCatalogPage("all", "", 1)).thenReturn(catalogPage);
        when(httpServletRequest.getHeader("X-Requested-With")).thenReturn(null);

        ConcurrentModel model = new ConcurrentModel();

        String viewName = productController.products(null, "all", "", 1, httpServletRequest, model);

        assertThat(viewName).isEqualTo("pages/products");
        assertThat(model.getAttribute("catalogPage")).isEqualTo(catalogPage);
        assertThat(model.getAttribute("isPlatformAdmin")).isEqualTo(false);
    }

    @Test
    void rendersProductListFragmentForAjaxRequests() {
        ProductCatalogPageView catalogPage = new ProductCatalogPageView(
                "PAPER",
                "Paper",
                "a4",
                1,
                6,
                1,
                1,
                List.of(1),
                false,
                false,
                1,
                1,
                false,
                false,
                1,
                1,
                List.of(new ProductCategoryFilterView("PAPER", "Paper", 1, true)),
                List.of()
        );
        when(productCatalogService.getCatalogPage("PAPER", "a4", 1)).thenReturn(catalogPage);
        when(httpServletRequest.getHeader("X-Requested-With")).thenReturn("XMLHttpRequest");

        ConcurrentModel model = new ConcurrentModel();

        String viewName = productController.products(null, "PAPER", "a4", 1, httpServletRequest, model);

        assertThat(viewName).isEqualTo("pages/products :: catalogPageContent");
        assertThat(model.getAttribute("catalogPage")).isEqualTo(catalogPage);
        assertThat(model.getAttribute("isPlatformAdmin")).isEqualTo(false);
    }

    @Test
    void rendersProductDetailPage() {
        ProductDetailView detail = new ProductDetailView(
                3L,
                "SKU-3",
                "Barcode Scanner",
                "ScanWorks",
                "Fixed barcode scanner for warehouse stations.",
                "Scanners",
                "Logistics Equipment",
                "/images/products/sku-3.png",
                new BigDecimal("540000.00"),
                "KRW",
                1
        );
        when(productCatalogService.getActiveProduct(3L)).thenReturn(detail);

        ConcurrentModel model = new ConcurrentModel();

        String viewName = productController.productDetail(3L, null, model);

        assertThat(viewName).isEqualTo("pages/product-detail");
        assertThat(model.getAttribute("product")).isEqualTo(detail);
        assertThat(model.getAttribute("isPlatformAdmin")).isEqualTo(false);
    }
}
