package com.bobbo01.supplyhub.domain.company.controller;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.service.CompanyMembershipService;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceMembershipControllerTest {

    @Mock
    private CompanyMembershipService companyMembershipService;

    @InjectMocks
    private WorkspaceMembershipController workspaceMembershipController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void leavesCompanyAndRedirectsHome() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setSession(new MockHttpSession());
        AuthenticatedUserPrincipal principal = principal("alice@example.com", "Alice");
        User detachedUser = User.builder()
                .company(null)
                .role(Role.builder().roleName(RoleNames.CART_USER).description("cart user").build())
                .companyAdmin(false)
                .email("alice@example.com")
                .fullName("Alice")
                .phone(null)
                .status("ACTIVE")
                .lastLoginAt(null)
                .build();

        when(companyMembershipService.leaveCompany(principal.getUserId())).thenReturn(detachedUser);

        String viewName = workspaceMembershipController.leave(principal, request, response);

        assertThat(viewName).isEqualTo("redirect:/");
        verify(companyMembershipService).leaveCompany(principal.getUserId());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    private AuthenticatedUserPrincipal principal(String email, String name) {
        return AuthenticatedUserPrincipal.from(
                User.createOAuthUser(
                        Company.builder().companyName("Example").status("ACTIVE").build(),
                        Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                        email,
                        name,
                        null
                ),
                java.util.List.of()
        );
    }
}
