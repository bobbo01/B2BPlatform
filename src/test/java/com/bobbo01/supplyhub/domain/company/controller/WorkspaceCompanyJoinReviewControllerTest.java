package com.bobbo01.supplyhub.domain.company.controller;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.service.CompanyJoinRequestService;
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
class WorkspaceCompanyJoinReviewControllerTest {

    @Mock
    private CompanyJoinRequestService companyJoinRequestService;

    @InjectMocks
    private WorkspaceCompanyJoinReviewController workspaceCompanyJoinReviewController;

    @Test
    void forwardsReviewMemoWhenApprovingCompanyJoinRequest() {
        AuthenticatedUserPrincipal principal = principal("admin@example.com", "Admin");

        String viewName = workspaceCompanyJoinReviewController.approveCompanyJoinRequest(principal, 10L, "approved");

        assertThat(viewName).isEqualTo("redirect:/workspace");
        verify(companyJoinRequestService).approveRequest(principal.getUserId(), 10L, "approved");
    }

    @Test
    void forwardsRejectionReasonWhenRejectingCompanyJoinRequest() {
        AuthenticatedUserPrincipal principal = principal("admin@example.com", "Admin");

        String viewName = workspaceCompanyJoinReviewController.rejectCompanyJoinRequest(
                principal,
                10L,
                "needs more info",
                "Domain mismatch"
        );

        assertThat(viewName).isEqualTo("redirect:/workspace");
        verify(companyJoinRequestService).rejectRequest(principal.getUserId(), 10L, "needs more info", "Domain mismatch");
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
