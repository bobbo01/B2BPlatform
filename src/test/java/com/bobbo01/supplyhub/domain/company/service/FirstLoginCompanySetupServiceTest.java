package com.bobbo01.supplyhub.domain.company.service;

import com.bobbo01.supplyhub.domain.company.dto.CompanyJoinRequestView;
import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.repository.CompanyRepository;
import com.bobbo01.supplyhub.domain.company.workflow.FirstLoginCompanySetupState;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.service.UserOAuthAccountService;
import com.bobbo01.supplyhub.global.auth.oauth.OAuth2AccountService;
import com.bobbo01.supplyhub.global.auth.oauth.OAuth2Attributes;
import com.bobbo01.supplyhub.global.auth.oauth.OAuth2LoginPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpSession;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirstLoginCompanySetupServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyRegistrationRequestService companyRegistrationRequestService;

    @Mock
    private CompanyJoinRequestService companyJoinRequestService;

    @Mock
    private CompanyInviteCodeService companyInviteCodeService;

    @Mock
    private UserOAuthAccountService userOAuthAccountService;

    @Mock
    private OAuth2AccountService oauth2AccountService;

    @Mock
    private OAuth2LoginPolicy loginPolicy;

    @InjectMocks
    private FirstLoginCompanySetupService firstLoginCompanySetupService;

    @Test
    void storesPendingStateAndSubmitsRegistrationRequestForFirstLogin() {
        MockHttpSession session = new MockHttpSession();
        OAuth2Attributes attributes = new OAuth2Attributes(
                "workspace",
                "provider-user-1",
                "alice@example.com",
                true,
                "Alice",
                "010-1111-2222",
                "alice@example.com",
                Map.of(),
                "sub"
        );

        when(oauth2AccountService.extractRequiredDomain(attributes)).thenReturn("example.com");
        when(oauth2AccountService.loginPolicy()).thenReturn(loginPolicy);
        when(loginPolicy.isPublicEmailDomain("example.com")).thenReturn(false);
        when(companyRepository.existsByCompanyDomainIgnoreCase("portal.example.com")).thenReturn(false);

        firstLoginCompanySetupService.storePendingState(session, attributes);
        firstLoginCompanySetupService.submitRegistrationRequest(session, "Example", "portal.example.com");

        assertThat(session.getAttribute(FirstLoginCompanySetupService.SESSION_ATTRIBUTE)).isNotNull();
        verify(companyRegistrationRequestService).submitRequest(
                any(FirstLoginCompanySetupState.class),
                eq("Example"),
                eq("portal.example.com")
        );
    }

    @Test
    void submitsJoinRequestByInviteCode() {
        MockHttpSession session = new MockHttpSession();
        OAuth2Attributes attributes = new OAuth2Attributes(
                "workspace",
                "provider-user-1",
                "alice@gmail.com",
                true,
                "Alice",
                null,
                "alice@gmail.com",
                Map.of(),
                "sub"
        );
        Company company = Company.builder()
                .companyName("Example")
                .companyDomain("example.com")
                .inviteCode("INVITE-123")
                .status("ACTIVE")
                .build();

        when(oauth2AccountService.extractRequiredDomain(attributes)).thenReturn("gmail.com");
        when(oauth2AccountService.loginPolicy()).thenReturn(loginPolicy);
        when(loginPolicy.isPublicEmailDomain("gmail.com")).thenReturn(true);
        when(companyRepository.findByInviteCodeIgnoreCase("INVITE-123")).thenReturn(Optional.of(company));

        firstLoginCompanySetupService.storePendingState(session, attributes);
        firstLoginCompanySetupService.submitJoinRequest(session, "INVITE-123");

        verify(companyJoinRequestService).submitJoinRequest(
                eq(company),
                eq("workspace"),
                eq("provider-user-1"),
                eq("alice@gmail.com"),
                eq("Alice")
        );
    }

    @Test
    void submitsRegistrationRequestForPublicEmailDomainWithoutInviteCode() {
        MockHttpSession session = new MockHttpSession();
        OAuth2Attributes attributes = new OAuth2Attributes(
                "workspace",
                "provider-user-1",
                "alice@gmail.com",
                true,
                "Alice",
                null,
                "alice@gmail.com",
                Map.of(),
                "sub"
        );

        when(oauth2AccountService.extractRequiredDomain(attributes)).thenReturn("gmail.com");
        when(oauth2AccountService.loginPolicy()).thenReturn(loginPolicy);
        when(loginPolicy.isPublicEmailDomain("gmail.com")).thenReturn(true);
        when(companyRepository.existsByCompanyDomainIgnoreCase("gmail.com")).thenReturn(false);

        firstLoginCompanySetupService.storePendingState(session, attributes);
        firstLoginCompanySetupService.submitRegistrationRequest(session, "Example", null);

        verify(companyRegistrationRequestService).submitRequest(
                any(FirstLoginCompanySetupState.class),
                eq("Example"),
                eq("gmail.com")
        );
    }

    @Test
    void rejectsCompanyRegistrationWhenDomainAlreadyExists() {
        MockHttpSession session = new MockHttpSession();
        OAuth2Attributes attributes = new OAuth2Attributes(
                "workspace",
                "provider-user-1",
                "alice@example.com",
                true,
                "Alice",
                null,
                "alice@example.com",
                Map.of(),
                "sub"
        );

        when(oauth2AccountService.extractRequiredDomain(attributes)).thenReturn("example.com");
        when(oauth2AccountService.loginPolicy()).thenReturn(loginPolicy);
        when(loginPolicy.isPublicEmailDomain("example.com")).thenReturn(false);
        when(companyRepository.existsByCompanyDomainIgnoreCase("portal.example.com")).thenReturn(true);

        firstLoginCompanySetupService.storePendingState(session, attributes);

        assertThatThrownBy(() -> firstLoginCompanySetupService.submitRegistrationRequest(
                session,
                "Example",
                "portal.example.com"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Company domain is already in use.");
    }

    @Test
    void rejectsExpiredPendingState() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(FirstLoginCompanySetupService.SESSION_ATTRIBUTE, new FirstLoginCompanySetupState(
                "workspace",
                "provider-user-1",
                "alice@example.com",
                "example.com",
                false,
                "Alice",
                null,
                true,
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(10),
                false
        ));

        assertThatThrownBy(() -> firstLoginCompanySetupService.getRequiredState(session))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");

        verify(companyRepository, never()).findByCompanyDomainIgnoreCase(any());
    }

    @Test
    void findsLatestJoinRequestForPendingState() {
        MockHttpSession session = new MockHttpSession();
        OAuth2Attributes attributes = new OAuth2Attributes(
                "workspace",
                "provider-user-1",
                "alice@example.com",
                true,
                "Alice",
                null,
                "alice@example.com",
                Map.of(),
                "sub"
        );
        CompanyJoinRequestView requestView = new CompanyJoinRequestView(
                1L,
                "Example",
                "example.com",
                "Alice",
                "alice@example.com",
                "PENDING",
                "CART_USER",
                "카트 사용자",
                null,
                null
        );

        when(oauth2AccountService.extractRequiredDomain(attributes)).thenReturn("example.com");
        when(oauth2AccountService.loginPolicy()).thenReturn(loginPolicy);
        when(loginPolicy.isPublicEmailDomain("example.com")).thenReturn(false);
        when(companyJoinRequestService.findLatestRequest("workspace", "provider-user-1")).thenReturn(Optional.of(requestView));

        firstLoginCompanySetupService.storePendingState(session, attributes);

        assertThat(firstLoginCompanySetupService.findLatestJoinRequest(session)).contains(requestView);
    }

    @Test
    void completesSetupByLookingUpRequestedCompanyDomainAfterConcurrentCreateConflict() {
        MockHttpSession session = new MockHttpSession();
        OAuth2Attributes attributes = new OAuth2Attributes(
                "workspace",
                "provider-user-1",
                "alice@example.com",
                true,
                "Alice",
                null,
                "alice@example.com",
                Map.of(),
                "sub"
        );
        Company company = Company.builder()
                .companyName("Example")
                .companyDomain("portal.example.com")
                .inviteCode("INVITE-123")
                .status("ACTIVE")
                .build();
        Role role = Role.builder().roleName(RoleNames.CART_USER).description("cart user").build();
        User user = User.createOAuthUser(company, role, "alice@example.com", "Alice", null);

        when(oauth2AccountService.extractRequiredDomain(attributes)).thenReturn("example.com");
        when(oauth2AccountService.loginPolicy()).thenReturn(loginPolicy);
        when(loginPolicy.isPublicEmailDomain("example.com")).thenReturn(false);
        when(companyRepository.findByCompanyDomainIgnoreCase("example.com")).thenReturn(Optional.empty());
        when(companyRepository.existsByCompanyDomainIgnoreCase("portal.example.com")).thenReturn(false);
        when(companyRepository.save(any(Company.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(companyRepository.findByCompanyDomainIgnoreCase("portal.example.com")).thenReturn(Optional.of(company));
        when(companyInviteCodeService.generateUniqueInviteCode()).thenReturn("INVITE-123");
        when(companyInviteCodeService.ensureInviteCode(company)).thenReturn(company);
        when(userOAuthAccountService.resolveUserAfterCompanySetup(eq(company), any(OAuth2Attributes.class), eq(loginPolicy)))
                .thenReturn(user);

        firstLoginCompanySetupService.storePendingState(session, attributes);

        User resolvedUser = firstLoginCompanySetupService.completeSetup(
                session,
                "Example",
                "portal.example.com",
                null,
                null
        );

        assertThat(resolvedUser).isSameAs(user);
        verify(companyRepository, times(1)).findByCompanyDomainIgnoreCase("portal.example.com");
    }
}
