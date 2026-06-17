package com.bobbo01.supplyhub.domain.product.controller;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.product.dto.ProductDetailView;
import com.bobbo01.supplyhub.domain.product.service.ProductCatalogService;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductDetailPageRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductCatalogService productCatalogService;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void hidesAddToCartFormForPlatformAdmin() throws Exception {
        AuthenticatedUserPrincipal principal = platformAdminPrincipal();
        when(productCatalogService.getActiveProduct(3L)).thenReturn(productDetailView());

        String content = mockMvc.perform(get("/products/3")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(
                                authenticatedToken(principal, "ROLE_PLATFORM_ADMIN")
                        )))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).doesNotContain("action=\"/commerce/cart/items\"");
        assertThat(content).doesNotContain("장바구니에 담기");
    }

    @Test
    void showsAddToCartFormForCompanyUser() throws Exception {
        AuthenticatedUserPrincipal principal = companyUserPrincipal();
        when(productCatalogService.getActiveProduct(3L)).thenReturn(productDetailView());

        String content = mockMvc.perform(get("/products/3")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(
                                authenticatedToken(principal, "ROLE_USER")
                        )))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).contains("action=\"/commerce/cart/items\"");
    }

    private ProductDetailView productDetailView() {
        return new ProductDetailView(
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
    }

    private AuthenticatedUserPrincipal platformAdminPrincipal() {
        User user = User.createPlatformAdmin(
                Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                "admin@example.com",
                "Admin",
                null
        );
        ReflectionTestUtils.setField(user, "id", 1L);
        return AuthenticatedUserPrincipal.from(user, List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN")));
    }

    private AuthenticatedUserPrincipal companyUserPrincipal() {
        User user = User.createOAuthUser(
                Company.builder().companyName("Example").status("ACTIVE").build(),
                Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                "user@example.com",
                "User",
                null
        );
        ReflectionTestUtils.setField(user, "id", 2L);
        return AuthenticatedUserPrincipal.from(user, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private TestingAuthenticationToken authenticatedToken(AuthenticatedUserPrincipal principal, String authority) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(authority))
        );
        authentication.setAuthenticated(true);
        return authentication;
    }
}
