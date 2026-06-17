package com.bobbo01.supplyhub.global.auth.oauth;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.service.FirstLoginCompanySetupService;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private OAuth2AccountService oauth2AccountService;

    @Mock
    private FirstLoginCompanySetupService firstLoginCompanySetupService;

    @InjectMocks
    private OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void redirectsToConfiguredPathWhenCompanyExists() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2Attributes attributes = new OAuth2Attributes(
                "workspace",
                "provider-user-1",
                "alice@example.com",
                true,
                "Alice",
                null,
                "alice@example.com",
                Map.of("sub", "provider-user-1", "email", "alice@example.com"),
                "sub"
        );
        Company company = Company.builder().companyName("Example").companyDomain("example.com").status("ACTIVE").build();
        Role role = Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build();
        User user = User.createPlatformAdmin(role, "alice@example.com", "Alice", null);
        CustomOAuth2User principal = CustomOAuth2User.pending(
                List.of(new SimpleGrantedAuthority("ROLE_FIRST_LOGIN_COMPANY_SETUP")),
                attributes.attributes(),
                attributes.nameAttributeKey(),
                attributes
        );
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "workspace");

        when(oauth2AccountService.resolveExistingUserByIdentity(attributes)).thenReturn(Optional.empty());
        when(oauth2AccountService.findActiveCompanyByEmailDomain(attributes)).thenReturn(Optional.of(company));
        when(oauth2AccountService.resolveUser(company, attributes)).thenReturn(user);

        oauth2LoginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo(OAuth2LoginSuccessHandler.POST_LOGIN_REDIRECT_PATH);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void keepsOAuthLoginWhenCompanyMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2Attributes attributes = new OAuth2Attributes(
                "workspace",
                "provider-user-1",
                "alice@example.com",
                true,
                "Alice",
                null,
                "alice@example.com",
                Map.of("sub", "provider-user-1", "email", "alice@example.com"),
                "sub"
        );
        CustomOAuth2User principal = CustomOAuth2User.pending(
                List.of(new SimpleGrantedAuthority("ROLE_FIRST_LOGIN_COMPANY_SETUP")),
                attributes.attributes(),
                attributes.nameAttributeKey(),
                attributes
        );
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "workspace");

        when(oauth2AccountService.resolveExistingUserByIdentity(attributes)).thenReturn(Optional.empty());
        when(oauth2AccountService.findActiveCompanyByEmailDomain(attributes)).thenReturn(Optional.empty());

        oauth2LoginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo(OAuth2LoginSuccessHandler.POST_LOGIN_REDIRECT_PATH);
        verify(firstLoginCompanySetupService).storePendingState(any(), eq(attributes));
        assertThat(request.getSession(false)).isNotNull();
        assertThat(request.getSession(false).getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                .isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
    }

    @Test
    void authenticatesExistingUserIdentityBeforeCompanyResolution() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2Attributes attributes = new OAuth2Attributes(
                "workspace",
                "provider-user-1",
                "alice@gmail.com",
                true,
                "Alice",
                null,
                "alice@gmail.com",
                Map.of("sub", "provider-user-1", "email", "alice@gmail.com"),
                "sub"
        );
        Company company = Company.builder().companyName("Existing Company").companyDomain("existing.com").status("ACTIVE").build();
        Role role = Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build();
        User user = User.createPlatformAdmin(role, "alice@gmail.com", "Alice", null);
        CustomOAuth2User principal = CustomOAuth2User.pending(
                List.of(new SimpleGrantedAuthority("ROLE_FIRST_LOGIN_COMPANY_SETUP")),
                attributes.attributes(),
                attributes.nameAttributeKey(),
                attributes
        );
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "workspace");

        when(oauth2AccountService.resolveExistingUserByIdentity(attributes)).thenReturn(Optional.of(user));

        oauth2LoginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo(OAuth2LoginSuccessHandler.POST_LOGIN_REDIRECT_PATH);
        verify(firstLoginCompanySetupService).clearPendingState(any());
    }

    @Test
    void bootstrapsPlatformAdminBeforeCompanyResolutionWhenEmailIsAllowlisted() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2Attributes attributes = new OAuth2Attributes(
                "workspace",
                "provider-user-1",
                "admin@example.com",
                true,
                "Admin",
                null,
                "admin@example.com",
                Map.of("sub", "provider-user-1", "email", "admin@example.com"),
                "sub"
        );
        Role role = Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build();
        User user = User.createPlatformAdmin(role, "admin@example.com", "Admin", null);
        CustomOAuth2User principal = CustomOAuth2User.pending(
                List.of(new SimpleGrantedAuthority("ROLE_FIRST_LOGIN_COMPANY_SETUP")),
                attributes.attributes(),
                attributes.nameAttributeKey(),
                attributes
        );
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "workspace");

        when(oauth2AccountService.resolveExistingUserByIdentity(attributes)).thenReturn(Optional.empty());
        when(oauth2AccountService.isPlatformAdminEmail(attributes)).thenReturn(true);
        when(oauth2AccountService.resolvePlatformAdmin(attributes)).thenReturn(user);

        oauth2LoginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo(OAuth2LoginSuccessHandler.POST_LOGIN_REDIRECT_PATH);
        verify(oauth2AccountService, never()).findActiveCompanyByEmailDomain(attributes);
        verify(firstLoginCompanySetupService).clearPendingState(any());
    }
}
