package com.bobbo01.supplyhub.domain.home.service;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.service.CompanyInviteCodeService;
import com.bobbo01.supplyhub.domain.product.dto.ProductSummaryView;
import com.bobbo01.supplyhub.domain.product.service.ProductCatalogService;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

    @Mock
    private CompanyInviteCodeService companyInviteCodeService;

    @Mock
    private ProductCatalogService productCatalogService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private HomeService homeService;

    @Test
    void returnsWorkspaceUserWithInviteCode() {
        Company company = Company.builder()
                .companyName("Example")
                .companyDomain("example.com")
                .inviteCode("INVITE1234")
                .status("ACTIVE")
                .build();
        Role role = Role.builder().roleName(RoleNames.CART_USER).description("default").build();
        User user = User.createOAuthUser(company, role, "alice@example.com", "Alice", null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var workspaceUser = homeService.getWorkspaceUser(1L);

        assertThat(workspaceUser.platformAdmin()).isFalse();
        assertThat(workspaceUser.roleLabel()).isEqualTo("일반 사용자");
        assertThat(workspaceUser.companyAdmin()).isFalse();
        assertThat(workspaceUser.company().inviteCode()).isEqualTo("INVITE1234");
    }

    @Test
    void returnsWorkspaceUserWithoutInviteCodeWhenInviteCodeWasRevoked() {
        Company company = Company.builder()
                .companyName("Example")
                .companyDomain("example.com")
                .inviteCode(null)
                .status("ACTIVE")
                .build();
        Role role = Role.builder().roleName(RoleNames.CART_USER).description("default").build();
        User user = User.createOAuthUser(company, role, "alice@example.com", "Alice", null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var workspaceUser = homeService.getWorkspaceUser(1L);

        assertThat(workspaceUser.company().inviteCode()).isNull();
    }

    @Test
    void returnsPlatformAdminWorkspaceWithoutCompany() {
        Role role = Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform").build();
        User user = User.createPlatformAdmin(role, "admin@example.com", "Admin", null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var workspaceUser = homeService.getWorkspaceUser(1L);

        assertThat(workspaceUser.platformAdmin()).isTrue();
        assertThat(workspaceUser.roleLabel()).isEqualTo("플랫폼 관리자");
        assertThat(workspaceUser.companyAdmin()).isFalse();
        assertThat(workspaceUser.company()).isNull();
    }

    @Test
    void returnsFeaturedProductsFromProductCatalogService() {
        ProductSummaryView featuredProduct = new ProductSummaryView(
                1L,
                "SKU-1",
                "Wireless Mouse",
                "SupplyTech",
                "Stationery",
                "/images/products/sku-1.png",
                new BigDecimal("320000.00"),
                "KRW",
                3
        );
        when(productCatalogService.getFeaturedProducts(6)).thenReturn(List.of(featuredProduct));

        var featuredProducts = homeService.getFeaturedProducts();

        assertThat(featuredProducts).containsExactly(featuredProduct);
    }
}
