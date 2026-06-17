package com.bobbo01.supplyhub.domain.company.controller;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.service.CompanyUserAdminService;
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
class WorkspaceCompanyUserAdminControllerTest {

    @Mock
    private CompanyUserAdminService companyUserAdminService;

    @InjectMocks
    private WorkspaceCompanyUserAdminController workspaceCompanyUserAdminController;

    @Test
    void forwardsPurchasingRoleUpdateForCompanyAdminWorkspace() {
        AuthenticatedUserPrincipal principal = principal("admin@example.com", "Admin");

        String viewName = workspaceCompanyUserAdminController.updateCompanyUserPurchasingRole(principal, 10L, RoleNames.APPROVER);

        assertThat(viewName).isEqualTo("redirect:/workspace?section=company-users");
        verify(companyUserAdminService).updateUserPurchasingRole(principal.getUserId(), 10L, RoleNames.APPROVER);
    }

    @Test
    void forwardsStatusUpdateForCompanyAdminWorkspace() {
        AuthenticatedUserPrincipal principal = principal("admin@example.com", "Admin");

        String viewName = workspaceCompanyUserAdminController.updateCompanyUserStatus(principal, 10L, "INACTIVE");

        assertThat(viewName).isEqualTo("redirect:/workspace?section=company-users");
        verify(companyUserAdminService).updateUserStatus(principal.getUserId(), 10L, "INACTIVE");
    }

    @Test
    void forwardsCompanyAdminUpdateForCompanyAdminWorkspace() {
        AuthenticatedUserPrincipal principal = principal("admin@example.com", "Admin");

        String viewName = workspaceCompanyUserAdminController.updateCompanyUserCompanyAdmin(principal, 10L, true);

        assertThat(viewName).isEqualTo("redirect:/workspace?section=company-users");
        verify(companyUserAdminService).updateUserCompanyAdmin(principal.getUserId(), 10L, true);
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
