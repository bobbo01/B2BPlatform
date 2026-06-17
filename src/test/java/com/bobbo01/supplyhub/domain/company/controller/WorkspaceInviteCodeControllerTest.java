package com.bobbo01.supplyhub.domain.company.controller;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.service.CompanyInviteCodeService;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkspaceInviteCodeControllerTest {

    @Mock
    private CompanyInviteCodeService companyInviteCodeService;

    @InjectMocks
    private WorkspaceInviteCodeController workspaceInviteCodeController;

    @Test
    void forwardsInviteCodeRegenerationForCompanyAdminWorkspace() {
        AuthenticatedUserPrincipal principal = principal("admin@example.com", "Admin");

        String viewName = workspaceInviteCodeController.regenerateCompanyInviteCode(principal);

        assertThat(viewName).isEqualTo("redirect:/workspace?section=overview");
        verify(companyInviteCodeService).regenerateInviteCode(principal.getUserId());
    }

    @Test
    void forwardsInviteCodeRevocationForCompanyAdminWorkspace() {
        AuthenticatedUserPrincipal principal = principal("admin@example.com", "Admin");

        String viewName = workspaceInviteCodeController.revokeCompanyInviteCode(principal);

        assertThat(viewName).isEqualTo("redirect:/workspace?section=overview");
        verify(companyInviteCodeService).revokeInviteCode(principal.getUserId());
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
