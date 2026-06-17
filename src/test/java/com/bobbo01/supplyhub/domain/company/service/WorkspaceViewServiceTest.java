package com.bobbo01.supplyhub.domain.company.service;

import com.bobbo01.supplyhub.domain.company.dto.FirstCompanyAdminRequestView;
import com.bobbo01.supplyhub.domain.commerce.dto.CommerceWorkspaceView;
import com.bobbo01.supplyhub.domain.commerce.service.CommerceWorkflowService;
import com.bobbo01.supplyhub.domain.home.dto.WorkspaceCompanyView;
import com.bobbo01.supplyhub.domain.home.dto.WorkspaceUserView;
import com.bobbo01.supplyhub.domain.home.service.HomeService;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceViewServiceTest {

    @Mock
    private FirstCompanyAdminRequestService firstCompanyAdminRequestService;

    @Mock
    private CompanyJoinRequestService companyJoinRequestService;

    @Mock
    private CompanyUserAdminService companyUserAdminService;

    @Mock
    private HomeService homeService;

    @Mock
    private CommerceWorkflowService commerceWorkflowService;

    @InjectMocks
    private WorkspaceViewService workspaceViewService;

    @Test
    void defaultsCompanyAdminWorkspaceToOverviewSection() {
        when(homeService.getWorkspaceUser(1L)).thenReturn(workspaceUserView(1L, RoleNames.CART_USER, true));
        when(firstCompanyAdminRequestService.canCreateRequest(1L)).thenReturn(false);
        when(firstCompanyAdminRequestService.findLatestRequestForUser(1L)).thenReturn(Optional.empty());
        when(companyJoinRequestService.getPendingRequestsForCompany(1L)).thenReturn(java.util.List.of());
        when(companyUserAdminService.getCompanyUserViews(1L)).thenReturn(java.util.List.of());
        when(companyUserAdminService.getPurchasingRoleOptions()).thenReturn(java.util.List.of(RoleNames.CART_USER));
        when(commerceWorkflowService.getWorkspaceView(1L)).thenReturn(commerceView(false));

        var workspacePageView = workspaceViewService.getWorkspacePage(1L, null, null, null);

        assertThat(workspacePageView.activeSection()).isEqualTo("overview");
        assertThat(workspacePageView.activeCommerceSection()).isEqualTo("cart");
        assertThat(workspacePageView.companyUserPurchasingRoleOptions()).containsExactly(RoleNames.CART_USER);
    }

    @Test
    void loadsCompanyUsersSectionForCompanyAdmin() {
        when(homeService.getWorkspaceUser(1L)).thenReturn(workspaceUserView(1L, RoleNames.CART_USER, true));
        when(firstCompanyAdminRequestService.canCreateRequest(1L)).thenReturn(false);
        when(firstCompanyAdminRequestService.findLatestRequestForUser(1L)).thenReturn(Optional.of(
                new FirstCompanyAdminRequestView(10L, 1L, "Example", "example.com", 1L, "User", "user@example.com", "PENDING")
        ));
        when(companyJoinRequestService.getPendingRequestsForCompany(1L)).thenReturn(java.util.List.of());
        when(companyUserAdminService.getCompanyUserViews(1L)).thenReturn(java.util.List.of());
        when(companyUserAdminService.getPurchasingRoleOptions()).thenReturn(java.util.List.of(RoleNames.CART_USER, RoleNames.APPROVER));
        when(commerceWorkflowService.getWorkspaceView(1L)).thenReturn(commerceView(false));

        var workspacePageView = workspaceViewService.getWorkspacePage(1L, "company-users", null, null);

        assertThat(workspacePageView.activeSection()).isEqualTo("company-users");
        assertThat(workspacePageView.firstCompanyAdminRequest()).isNotNull();
        assertThat(workspacePageView.companyUserPurchasingRoleOptions()).containsExactly(RoleNames.CART_USER, RoleNames.APPROVER);
    }

    @Test
    void fallsBackFromApprovalsWhenUserCannotApprove() {
        when(homeService.getWorkspaceUser(1L)).thenReturn(workspaceUserView(1L, RoleNames.CART_USER, false));
        when(firstCompanyAdminRequestService.canCreateRequest(1L)).thenReturn(false);
        when(firstCompanyAdminRequestService.findLatestRequestForUser(1L)).thenReturn(Optional.empty());
        when(commerceWorkflowService.getWorkspaceView(1L)).thenReturn(commerceView(false));

        var workspacePageView = workspaceViewService.getWorkspacePage(1L, "commerce", "approvals", null);

        assertThat(workspacePageView.activeSection()).isEqualTo("commerce");
        assertThat(workspacePageView.activeCommerceSection()).isEqualTo("cart");
    }

    @Test
    void keepsSelectedPurchaseOrderIdForOrderDraftSection() {
        when(homeService.getWorkspaceUser(1L)).thenReturn(workspaceUserView(1L, RoleNames.PURCHASER, false));
        when(firstCompanyAdminRequestService.canCreateRequest(1L)).thenReturn(false);
        when(firstCompanyAdminRequestService.findLatestRequestForUser(1L)).thenReturn(Optional.empty());
        when(commerceWorkflowService.getWorkspaceView(1L)).thenReturn(commerceView(false));

        var workspacePageView = workspaceViewService.getWorkspacePage(1L, "commerce", "order-drafts", 99L);

        assertThat(workspacePageView.activeCommerceSection()).isEqualTo("order-drafts");
        assertThat(workspacePageView.selectedPurchaseOrderId()).isEqualTo(99L);
    }

    @Test
    void ignoresSelectedPurchaseOrderIdOutsideOrderDraftSection() {
        when(homeService.getWorkspaceUser(1L)).thenReturn(workspaceUserView(1L, RoleNames.PURCHASER, false));
        when(firstCompanyAdminRequestService.canCreateRequest(1L)).thenReturn(false);
        when(firstCompanyAdminRequestService.findLatestRequestForUser(1L)).thenReturn(Optional.empty());
        when(commerceWorkflowService.getWorkspaceView(1L)).thenReturn(commerceView(false));

        var workspacePageView = workspaceViewService.getWorkspacePage(1L, "commerce", "cart", 99L);

        assertThat(workspacePageView.selectedPurchaseOrderId()).isNull();
    }

    private WorkspaceUserView workspaceUserView(Long userId, String roleName, boolean companyAdmin) {
        return new WorkspaceUserView(
                userId,
                "User",
                "user@example.com",
                false,
                roleName,
                companyAdmin,
                new WorkspaceCompanyView(1L, "Example", "example.com", "INVITE", "ACTIVE")
        );
    }

    private CommerceWorkspaceView commerceView(boolean canApprove) {
        return new CommerceWorkspaceView(
                true,
                true,
                canApprove,
                null,
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of()
        );
    }
}
