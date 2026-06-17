package com.bobbo01.supplyhub.domain.admin.controller;

import com.bobbo01.supplyhub.domain.admin.dto.PlatformAdminPageView;
import com.bobbo01.supplyhub.domain.admin.dto.PlatformSettlementSummaryView;
import com.bobbo01.supplyhub.domain.admin.service.PlatformAdminPageService;
import com.bobbo01.supplyhub.domain.admin.service.PlatformAdminService;
import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.service.CompanyRegistrationRequestService;
import com.bobbo01.supplyhub.domain.company.service.FirstCompanyAdminRequestService;
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
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformAdminControllerTest {

    @Mock
    private PlatformAdminPageService platformAdminPageService;

    @Mock
    private PlatformAdminService platformAdminService;

    @Mock
    private CompanyRegistrationRequestService companyRegistrationRequestService;

    @Mock
    private FirstCompanyAdminRequestService firstCompanyAdminRequestService;

    @InjectMocks
    private PlatformAdminController platformAdminController;

    @Test
    void redirectsNonPlatformAdminWorkspaceRequestToWorkspace() {
        AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(
                User.createOAuthUser(
                        Company.builder().companyName("Example").companyDomain("example.com").status("ACTIVE").build(),
                        Role.builder().roleName(RoleNames.CART_USER).description("cart user").build(),
                        "user@example.com",
                        "User",
                        null
                ),
                java.util.List.of()
        );

        when(platformAdminPageService.isPlatformAdmin(principal.getUserId())).thenReturn(false);

        String viewName = platformAdminController.companies(principal, new ConcurrentModel());

        assertThat(viewName).isEqualTo("redirect:/workspace");
    }

    @Test
    void redirectsAdminRootToCompanySection() {
        String viewName = platformAdminController.adminHome();

        assertThat(viewName).isEqualTo("redirect:/admin/companies");
    }

    @Test
    void updatesUserStatusFromPlatformAdminAction() {
        AuthenticatedUserPrincipal principal = platformAdminPrincipal();

        String viewName = platformAdminController.updateUserStatus(principal, 10L, "INACTIVE");

        assertThat(viewName).isEqualTo("redirect:/admin/users");
        verify(platformAdminService).updateUserStatus(principal.getUserId(), 10L, "INACTIVE");
    }

    @Test
    void approvesCompanyRegistrationRequestFromPlatformAdminAction() {
        AuthenticatedUserPrincipal principal = platformAdminPrincipal();

        String viewName = platformAdminController.approveCompanyRegistrationRequest(principal, 15L, "approve memo");

        assertThat(viewName).isEqualTo("redirect:/admin/companies");
        verify(companyRegistrationRequestService).approveRequest(principal.getUserId(), 15L, "approve memo");
    }

    @Test
    void loadsOrdersSectionForPlatformAdmin() {
        AuthenticatedUserPrincipal principal = platformAdminPrincipal();
        ConcurrentModel model = new ConcurrentModel();
        when(platformAdminPageService.isPlatformAdmin(principal.getUserId())).thenReturn(true);
        when(platformAdminPageService.getOrdersPage(principal.getUserId(), null)).thenReturn(
                pageView("orders", "pending")
        );

        String viewName = platformAdminController.orders(principal, null, model);

        assertThat(viewName).isEqualTo("pages/admin");
        assertThat(model.getAttribute("activeSection")).isEqualTo("orders");
        assertThat(model.getAttribute("activeOrderFilter")).isEqualTo("pending");
    }

    @Test
    void forwardsOrderApprovalFromPlatformAdminAction() {
        AuthenticatedUserPrincipal principal = platformAdminPrincipal();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String viewName = platformAdminController.approvePurchaseOrder(principal, 15L, "ok", redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/admin/orders?orderFilter=approved");
        assertThat(redirectAttributes.getFlashAttributes().get("adminAlert")).isEqualTo("주문을 승인하고 결제 대기 상태로 전환했습니다.");
        verify(platformAdminService).approvePurchaseOrder(principal.getUserId(), 15L, "ok");
    }

    @Test
    void forwardsOrderRejectionFromPlatformAdminAction() {
        AuthenticatedUserPrincipal principal = platformAdminPrincipal();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String viewName = platformAdminController.rejectPurchaseOrder(principal, 15L, "memo", "out of policy", redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/admin/orders?orderFilter=rejected");
        assertThat(redirectAttributes.getFlashAttributes().get("adminAlert")).isEqualTo("주문을 반려했습니다.");
        verify(platformAdminService).rejectPurchaseOrder(principal.getUserId(), 15L, "memo", "out of policy");
    }

    @Test
    void loadsDeliveryOrdersSectionForPlatformAdmin() {
        AuthenticatedUserPrincipal principal = platformAdminPrincipal();
        ConcurrentModel model = new ConcurrentModel();
        when(platformAdminPageService.isPlatformAdmin(principal.getUserId())).thenReturn(true);
        when(platformAdminPageService.getOrdersPage(principal.getUserId(), "delivery")).thenReturn(
                pageView("orders", "delivery")
        );

        String viewName = platformAdminController.orders(principal, "delivery", model);

        assertThat(viewName).isEqualTo("pages/admin");
        assertThat(model.getAttribute("activeOrderFilter")).isEqualTo("delivery");
    }

    @Test
    void loadsAllOrdersSectionForPlatformAdmin() {
        AuthenticatedUserPrincipal principal = platformAdminPrincipal();
        ConcurrentModel model = new ConcurrentModel();
        when(platformAdminPageService.isPlatformAdmin(principal.getUserId())).thenReturn(true);
        when(platformAdminPageService.getOrdersPage(principal.getUserId(), "all")).thenReturn(
                pageView("orders", "all")
        );

        String viewName = platformAdminController.orders(principal, "all", model);

        assertThat(viewName).isEqualTo("pages/admin");
        assertThat(model.getAttribute("activeSection")).isEqualTo("orders");
        assertThat(model.getAttribute("activeOrderFilter")).isEqualTo("all");
    }

    @Test
    void loadsSettlementsSectionForPlatformAdmin() {
        AuthenticatedUserPrincipal principal = platformAdminPrincipal();
        ConcurrentModel model = new ConcurrentModel();
        when(platformAdminPageService.isPlatformAdmin(principal.getUserId())).thenReturn(true);
        when(platformAdminPageService.getSettlementsPage(principal.getUserId())).thenReturn(
                new PlatformAdminPageView(
                        new WorkspaceUserView(principal.getUserId(), "Admin", "admin@example.com", true, "PLATFORM_ADMIN", false, null),
                        "settlements",
                        PlatformAdminService.ORDER_FILTER_PENDING,
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        new PlatformSettlementSummaryView(
                                java.math.BigDecimal.ZERO,
                                java.math.BigDecimal.ZERO,
                                java.math.BigDecimal.ZERO,
                                0,
                                0,
                                0
                        ),
                        java.util.List.of()
                )
        );

        String viewName = platformAdminController.settlements(principal, model);

        assertThat(viewName).isEqualTo("pages/admin");
        assertThat(model.getAttribute("activeSection")).isEqualTo("settlements");
    }

    @Test
    void forwardsDeliveryActionsFromPlatformAdminAction() {
        AuthenticatedUserPrincipal principal = platformAdminPrincipal();
        RedirectAttributesModelMap readyAttributes = new RedirectAttributesModelMap();
        RedirectAttributesModelMap shippedAttributes = new RedirectAttributesModelMap();
        RedirectAttributesModelMap deliveredAttributes = new RedirectAttributesModelMap();

        assertThat(platformAdminController.markOrderReadyToShip(principal, 10L, readyAttributes))
                .isEqualTo("redirect:/admin/orders?orderFilter=delivery");
        assertThat(platformAdminController.markOrderShipped(principal, 11L, shippedAttributes))
                .isEqualTo("redirect:/admin/orders?orderFilter=delivery");
        assertThat(platformAdminController.markOrderDelivered(principal, 12L, deliveredAttributes))
                .isEqualTo("redirect:/admin/orders?orderFilter=delivery");
        assertThat(readyAttributes.getFlashAttributes().get("adminAlert")).isEqualTo("주문을 배송 준비 상태로 전환했습니다.");
        assertThat(shippedAttributes.getFlashAttributes().get("adminAlert")).isEqualTo("주문을 배송 중 상태로 전환했습니다.");
        assertThat(deliveredAttributes.getFlashAttributes().get("adminAlert")).isEqualTo("주문을 배송 완료 상태로 전환했습니다.");

        verify(platformAdminService).markOrderReadyToShip(principal.getUserId(), 10L);
        verify(platformAdminService).markOrderShipped(principal.getUserId(), 11L);
        verify(platformAdminService).markOrderDelivered(principal.getUserId(), 12L);
    }

    @Test
    void propagatesPlatformOrderActionErrorsToGlobalHandler() {
        AuthenticatedUserPrincipal principal = platformAdminPrincipal();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        doThrow(new IllegalStateException("forbidden"))
                .when(platformAdminService)
                .approvePurchaseOrder(principal.getUserId(), 15L, "ok");

        assertThatThrownBy(() -> platformAdminController.approvePurchaseOrder(principal, 15L, "ok", redirectAttributes))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("forbidden");
    }

    @Test
    void forwardsSettlementActionsFromPlatformAdminAction() {
        AuthenticatedUserPrincipal principal = platformAdminPrincipal();
        RedirectAttributesModelMap singleAttributes = new RedirectAttributesModelMap();
        RedirectAttributesModelMap bulkAttributes = new RedirectAttributesModelMap();

        assertThat(platformAdminController.markOrderSettled(principal, 15L, singleAttributes))
                .isEqualTo("redirect:/admin/settlements");
        assertThat(platformAdminController.markOrdersSettled(principal, java.util.List.of(15L, 16L), bulkAttributes))
                .isEqualTo("redirect:/admin/settlements");
        assertThat(singleAttributes.getFlashAttributes().get("adminAlert"))
                .isEqualTo("선택한 주문의 정산을 완료했습니다.");
        assertThat(bulkAttributes.getFlashAttributes().get("adminAlert"))
                .isEqualTo("선택한 주문들의 정산을 완료했습니다.");

        verify(platformAdminService).markOrderSettled(principal.getUserId(), 15L);
        verify(platformAdminService).markOrdersSettled(principal.getUserId(), java.util.List.of(15L, 16L));
    }

    private AuthenticatedUserPrincipal platformAdminPrincipal() {
        return AuthenticatedUserPrincipal.from(
                User.createPlatformAdmin(
                        Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build(),
                        "admin@example.com",
                        "Admin",
                        null
                ),
                java.util.List.of()
        );
    }

    private PlatformAdminPageView pageView(String activeSection, String activeOrderFilter) {
        return new PlatformAdminPageView(
                new WorkspaceUserView(1L, "Admin", "admin@example.com", true, "PLATFORM_ADMIN", false, null),
                activeSection,
                activeOrderFilter,
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                null,
                java.util.List.of()
        );
    }
}
