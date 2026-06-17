package com.bobbo01.supplyhub.domain.home.controller;

import com.bobbo01.supplyhub.domain.home.service.HomeService;
import com.bobbo01.supplyhub.domain.product.dto.ProductSummaryView;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import com.bobbo01.supplyhub.global.auth.oauth.CustomOAuth2User;
import com.bobbo01.supplyhub.global.auth.oauth.OAuth2Attributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    private static final ProductSummaryView FEATURED_PRODUCT = new ProductSummaryView(
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

    @Mock
    private HomeService homeService;

    @InjectMocks
    private HomeController homeController;

    @Test
    void marksLinkedUserAsLoggedIn() {
        Model model = new ConcurrentModel();
        Role role = Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build();
        AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(
                com.bobbo01.supplyhub.domain.user.entity.User.createPlatformAdmin(
                        role,
                        "linked@example.com",
                        "Linked User",
                        null
                ),
                List.of(new SimpleGrantedAuthority("ROLE_CART_USER"))
        );
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication =
                org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        when(homeService.getSsoProviders()).thenReturn(List.of());
        when(homeService.getFeaturedProducts()).thenReturn(List.of(FEATURED_PRODUCT));
        when(homeService.isLinkedUser(principal.getUserId())).thenReturn(true);
        when(homeService.getLinkedCompanyName(principal.getUserId())).thenReturn(null);

        String viewName = homeController.home(authentication, principal, null, model);

        assertThat(viewName).isEqualTo("pages/home");
        HomeController.HomeSessionUser user = (HomeController.HomeSessionUser) model.getAttribute("user");
        assertThat(model.getAttribute("isLoggedIn")).isEqualTo(true);
        assertThat(user.linkedUser()).isTrue();
        assertThat(user.email()).isEqualTo("linked@example.com");
    }

    @Test
    void marksPendingOAuthUserAsLoggedIn() {
        Model model = new ConcurrentModel();
        OAuth2Attributes attributes = new OAuth2Attributes(
                "workspace",
                "provider-user-1",
                "pending@example.com",
                true,
                "Pending User",
                null,
                "pending@example.com",
                Map.of("sub", "provider-user-1", "email", "pending@example.com"),
                "sub"
        );
        CustomOAuth2User principal = CustomOAuth2User.pending(
                List.of(new SimpleGrantedAuthority("ROLE_FIRST_LOGIN_COMPANY_SETUP")),
                attributes.attributes(),
                attributes.nameAttributeKey(),
                attributes
        );
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "workspace"
        );

        when(homeService.getSsoProviders()).thenReturn(List.of());
        when(homeService.getFeaturedProducts()).thenReturn(List.of(FEATURED_PRODUCT));

        String viewName = homeController.home(authentication, principal, null, model);

        assertThat(viewName).isEqualTo("pages/home");
        HomeController.HomeSessionUser user = (HomeController.HomeSessionUser) model.getAttribute("user");
        assertThat(model.getAttribute("isLoggedIn")).isEqualTo(true);
        assertThat(user.linkedUser()).isFalse();
        assertThat(user.displayName()).isEqualTo("Pending User");
        assertThat(user.email()).isEqualTo("pending@example.com");
    }

    @Test
    void marksAnonymousUserAsLoggedOut() {
        Model model = new ConcurrentModel();
        AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );

        when(homeService.getSsoProviders()).thenReturn(List.of());
        when(homeService.getFeaturedProducts()).thenReturn(List.of(FEATURED_PRODUCT));

        String viewName = homeController.home(authentication, null, null, model);

        assertThat(viewName).isEqualTo("pages/home");
        HomeController.HomeSessionUser user = (HomeController.HomeSessionUser) model.getAttribute("user");
        assertThat(model.getAttribute("isLoggedIn")).isEqualTo(false);
        assertThat(user.loggedIn()).isFalse();
    }

    @Test
    void marksDetachedAuthenticatedUserAsPending() {
        Model model = new ConcurrentModel();
        Role role = Role.builder().roleName(RoleNames.CART_USER).description("cart user").build();
        AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(
                com.bobbo01.supplyhub.domain.user.entity.User.builder()
                        .company(null)
                        .role(role)
                        .companyAdmin(false)
                        .email("detached@example.com")
                        .fullName("Detached User")
                        .phone(null)
                        .status("ACTIVE")
                        .lastLoginAt(null)
                        .build(),
                List.of()
        );
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication =
                org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        when(homeService.getSsoProviders()).thenReturn(List.of());
        when(homeService.getFeaturedProducts()).thenReturn(List.of(FEATURED_PRODUCT));
        when(homeService.isLinkedUser(principal.getUserId())).thenReturn(false);

        String viewName = homeController.home(authentication, principal, null, model);

        assertThat(viewName).isEqualTo("pages/home");
        HomeController.HomeSessionUser user = (HomeController.HomeSessionUser) model.getAttribute("user");
        assertThat(user.linkedUser()).isFalse();
        assertThat(user.email()).isEqualTo("detached@example.com");
    }

    @Test
    void exposesLoginErrorMessageForAlert() {
        Model model = new ConcurrentModel();
        AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );

        when(homeService.getSsoProviders()).thenReturn(List.of());
        when(homeService.getFeaturedProducts()).thenReturn(List.of(FEATURED_PRODUCT));

        String viewName = homeController.home(authentication, null, "user_inactive", model);

        assertThat(viewName).isEqualTo("pages/home");
        assertThat(model.getAttribute("loginErrorMessage")).isEqualTo("비활성 사용자 계정입니다.");
    }
}
