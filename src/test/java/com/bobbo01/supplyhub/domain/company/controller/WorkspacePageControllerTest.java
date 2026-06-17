package com.bobbo01.supplyhub.domain.company.controller;

import com.bobbo01.supplyhub.domain.company.dto.CompanyProfileUpdateForm;
import com.bobbo01.supplyhub.domain.company.dto.FirstLoginCompanySetupForm;
import com.bobbo01.supplyhub.domain.company.dto.WorkspacePageView;
import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.service.FirstLoginCompanySetupService;
import com.bobbo01.supplyhub.domain.company.service.WorkspaceViewService;
import com.bobbo01.supplyhub.domain.company.workflow.FirstLoginCompanySetupState;
import com.bobbo01.supplyhub.domain.commerce.dto.CommerceWorkspaceView;
import com.bobbo01.supplyhub.domain.home.dto.WorkspaceCompanyView;
import com.bobbo01.supplyhub.domain.home.dto.WorkspaceUserView;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspacePageControllerTest {

    @Mock
    private FirstLoginCompanySetupService firstLoginCompanySetupService;

    @Mock
    private WorkspaceViewService workspaceViewService;

    @InjectMocks
    private WorkspacePageController workspacePageController;

    @Test
    void prepopulatesCompanyDomainWithEmailDomainInRegisterMode() {
        MockHttpSession session = new MockHttpSession();
        ConcurrentModel model = new ConcurrentModel();
        FirstLoginCompanySetupState state = pendingSetupState("alice@example.com", "example.com", false);

        when(firstLoginCompanySetupService.getRequiredState(eq(session))).thenReturn(state);
        when(firstLoginCompanySetupService.findLatestJoinRequest(eq(session))).thenReturn(Optional.empty());

        String viewName = workspacePageController.workspace(null, "register", null, null, null, session, model);

        assertThat(viewName).isEqualTo("pages/workspace");
        FirstLoginCompanySetupForm form = (FirstLoginCompanySetupForm) model.getAttribute("form");
        assertThat(form).isNotNull();
        assertThat(form.getCompanyDomain()).isEqualTo("example.com");
    }

    @Test
    void showsRegisterModeForCompanyRegistrationPath() {
        MockHttpSession session = new MockHttpSession();
        ConcurrentModel model = new ConcurrentModel();
        FirstLoginCompanySetupState state = pendingSetupState("alice@example.com", "example.com", false);

        when(firstLoginCompanySetupService.getRequiredState(eq(session))).thenReturn(state);
        when(firstLoginCompanySetupService.findLatestJoinRequest(eq(session))).thenReturn(Optional.empty());

        String viewName = workspacePageController.workspace(null, "register", null, null, null, session, model);

        assertThat(viewName).isEqualTo("pages/workspace");
        assertThat(model.getAttribute("pendingSetup")).isEqualTo(true);
        assertThat(model.getAttribute("setupMode")).isEqualTo("register");
    }

    @Test
    void defaultsCompanyAdminWorkspaceToOverviewSection() {
        MockHttpSession session = new MockHttpSession();
        ConcurrentModel model = new ConcurrentModel();
        AuthenticatedUserPrincipal principal = principal("admin@example.com", "Admin");
        WorkspaceUserView workspaceUserView = workspaceUserView(principal.getUserId(), "Admin", "admin@example.com", RoleNames.CART_USER, true);
        when(workspaceViewService.getWorkspacePage(principal.getUserId(), null, null, null))
                .thenReturn(workspacePageView(workspaceUserView, "overview", "cart", null, java.util.List.of(RoleNames.CART_USER)));

        String viewName = workspacePageController.workspace(principal, null, null, null, null, session, model);

        assertThat(viewName).isEqualTo("pages/workspace");
        assertThat(model.getAttribute("activeSection")).isEqualTo("overview");
        assertThat(model.getAttribute("activeCommerceSection")).isEqualTo("cart");
    }

    @Test
    void loadsCompanyUsersSectionForCompanyAdmin() {
        MockHttpSession session = new MockHttpSession();
        ConcurrentModel model = new ConcurrentModel();
        AuthenticatedUserPrincipal principal = principal("admin@example.com", "Admin");
        WorkspaceUserView workspaceUserView = workspaceUserView(principal.getUserId(), "Admin", "admin@example.com", RoleNames.CART_USER, true);
        when(workspaceViewService.getWorkspacePage(principal.getUserId(), "company-users", null, null))
                .thenReturn(workspacePageView(
                        workspaceUserView,
                        "company-users",
                        "cart",
                        null,
                        java.util.List.of(RoleNames.CART_USER, RoleNames.APPROVER)
                ));

        String viewName = workspacePageController.workspace(principal, null, "company-users", null, null, session, model);

        assertThat(viewName).isEqualTo("pages/workspace");
        assertThat(model.getAttribute("activeSection")).isEqualTo("company-users");
        assertThat(model.getAttribute("companyUserAdminUsers")).isEqualTo(java.util.List.of());
        assertThat(model.getAttribute("companyUserPurchasingRoleOptions")).isEqualTo(java.util.List.of(RoleNames.CART_USER, RoleNames.APPROVER));
    }

    @Test
    void prepopulatesCompanyProfileFormForWorkspaceCompany() {
        MockHttpSession session = new MockHttpSession();
        ConcurrentModel model = new ConcurrentModel();
        AuthenticatedUserPrincipal principal = principal("admin@example.com", "Admin");
        WorkspaceUserView workspaceUserView = workspaceUserView(principal.getUserId(), "Admin", "admin@example.com", RoleNames.CART_USER, true);
        when(workspaceViewService.getWorkspacePage(principal.getUserId(), "overview", null, null))
                .thenReturn(workspacePageView(workspaceUserView, "overview", "cart", null, java.util.List.of()));

        String viewName = workspacePageController.workspace(principal, null, "overview", null, null, session, model);

        assertThat(viewName).isEqualTo("pages/workspace");
        CompanyProfileUpdateForm form = (CompanyProfileUpdateForm) model.getAttribute("companyProfileForm");
        assertThat(form).isNotNull();
        assertThat(form.getCompanyName()).isEqualTo("Example");
        assertThat(form.getCompanyDomain()).isEqualTo("example.com");
    }

    @Test
    void allowsExplicitCommerceSectionForRegularCompanyUser() {
        MockHttpSession session = new MockHttpSession();
        ConcurrentModel model = new ConcurrentModel();
        AuthenticatedUserPrincipal principal = principal("user@example.com", "User");
        WorkspaceUserView workspaceUserView = workspaceUserView(principal.getUserId(), "User", "user@example.com", RoleNames.CART_USER, false);
        when(workspaceViewService.getWorkspacePage(principal.getUserId(), "commerce", "purchase-requests", null))
                .thenReturn(workspacePageView(workspaceUserView, "commerce", "purchase-requests", null, java.util.List.of()));

        String viewName = workspacePageController.workspace(principal, null, "commerce", "purchase-requests", null, session, model);

        assertThat(viewName).isEqualTo("pages/workspace");
        assertThat(model.getAttribute("activeSection")).isEqualTo("commerce");
        assertThat(model.getAttribute("activeCommerceSection")).isEqualTo("purchase-requests");
    }

    @Test
    void fallsBackFromApprovalsWhenUserCannotApprove() {
        MockHttpSession session = new MockHttpSession();
        ConcurrentModel model = new ConcurrentModel();
        AuthenticatedUserPrincipal principal = principal("user@example.com", "User");
        WorkspaceUserView workspaceUserView = workspaceUserView(principal.getUserId(), "User", "user@example.com", RoleNames.CART_USER, false);
        when(workspaceViewService.getWorkspacePage(principal.getUserId(), "commerce", "approvals", null))
                .thenReturn(workspacePageView(workspaceUserView, "commerce", "cart", null, java.util.List.of()));

        String viewName = workspacePageController.workspace(principal, null, "commerce", "approvals", null, session, model);

        assertThat(viewName).isEqualTo("pages/workspace");
        assertThat(model.getAttribute("activeSection")).isEqualTo("commerce");
        assertThat(model.getAttribute("activeCommerceSection")).isEqualTo("cart");
    }

    @Test
    void exposesSelectedPurchaseOrderIdForOrderDraftSection() {
        MockHttpSession session = new MockHttpSession();
        ConcurrentModel model = new ConcurrentModel();
        AuthenticatedUserPrincipal principal = principal("user@example.com", "User");
        WorkspaceUserView workspaceUserView = workspaceUserView(principal.getUserId(), "User", "user@example.com", RoleNames.PURCHASER, false);
        when(workspaceViewService.getWorkspacePage(principal.getUserId(), "commerce", "order-drafts", 99L))
                .thenReturn(workspacePageView(
                        workspaceUserView,
                        "commerce",
                        "order-drafts",
                        99L,
                        java.util.List.of()
                ));

        String viewName = workspacePageController.workspace(principal, null, "commerce", "order-drafts", 99L, session, model);

        assertThat(viewName).isEqualTo("pages/workspace");
        assertThat(model.getAttribute("activeCommerceSection")).isEqualTo("order-drafts");
        assertThat(model.getAttribute("selectedPurchaseOrderId")).isEqualTo(99L);
    }

    @Test
    void doesNotExposeSelectedPurchaseOrderIdOutsideOrderDraftSection() {
        MockHttpSession session = new MockHttpSession();
        ConcurrentModel model = new ConcurrentModel();
        AuthenticatedUserPrincipal principal = principal("user@example.com", "User");
        WorkspaceUserView workspaceUserView = workspaceUserView(principal.getUserId(), "User", "user@example.com", RoleNames.PURCHASER, false);
        when(workspaceViewService.getWorkspacePage(principal.getUserId(), "commerce", "cart", 99L))
                .thenReturn(workspacePageView(workspaceUserView, "commerce", "cart", null, java.util.List.of()));

        String viewName = workspacePageController.workspace(principal, null, "commerce", "cart", 99L, session, model);

        assertThat(viewName).isEqualTo("pages/workspace");
        assertThat(model.getAttribute("activeCommerceSection")).isEqualTo("cart");
        assertThat(model.getAttribute("selectedPurchaseOrderId")).isNull();
    }

    @Test
    void allowsOrderDraftSectionForCartUserWithoutSelectedOrderId() {
        MockHttpSession session = new MockHttpSession();
        ConcurrentModel model = new ConcurrentModel();
        AuthenticatedUserPrincipal principal = principal("user@example.com", "User");
        WorkspaceUserView workspaceUserView = workspaceUserView(principal.getUserId(), "User", "user@example.com", RoleNames.CART_USER, false);
        when(workspaceViewService.getWorkspacePage(principal.getUserId(), "commerce", "order-drafts", null))
                .thenReturn(workspacePageView(workspaceUserView, "commerce", "order-drafts", null, java.util.List.of()));

        String viewName = workspacePageController.workspace(principal, null, "commerce", "order-drafts", null, session, model);

        assertThat(viewName).isEqualTo("pages/workspace");
        assertThat(model.getAttribute("activeSection")).isEqualTo("commerce");
        assertThat(model.getAttribute("activeCommerceSection")).isEqualTo("order-drafts");
        assertThat(model.getAttribute("selectedPurchaseOrderId")).isNull();
    }

    private FirstLoginCompanySetupState pendingSetupState(String email, String emailDomain, boolean publicDomain) {
        return new FirstLoginCompanySetupState(
                "workspace",
                "provider-user-1",
                email,
                emailDomain,
                publicDomain,
                "Alice",
                null,
                true,
                Instant.now(),
                Instant.now().plusSeconds(300),
                false
        );
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

    private WorkspacePageView workspacePageView(
            WorkspaceUserView workspaceUserView,
            String activeSection,
            String activeCommerceSection,
            Long selectedPurchaseOrderId,
            java.util.List<String> companyUserPurchasingRoleOptions
    ) {
        return new WorkspacePageView(
                workspaceUserView,
                activeSection,
                false,
                null,
                java.util.List.of(),
                java.util.List.of(),
                companyUserPurchasingRoleOptions,
                new CommerceWorkspaceView(
                        true,
                        true,
                        false,
                        null,
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of()
                ),
                activeCommerceSection,
                selectedPurchaseOrderId
        );
    }

    private WorkspaceUserView workspaceUserView(
            Long userId,
            String displayName,
            String email,
            String roleName,
            boolean companyAdmin
    ) {
        return new WorkspaceUserView(
                userId,
                displayName,
                email,
                false,
                roleName,
                companyAdmin,
                new WorkspaceCompanyView(1L, "Example", "example.com", "INVITE", "ACTIVE")
        );
    }
}
