package com.bobbo01.supplyhub.domain.commerce.controller;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.commerce.service.CommerceWorkflowService;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommerceControllerTest {

    @Mock
    private CommerceWorkflowService commerceWorkflowService;

    @InjectMocks
    private CommerceController commerceController;

    @Test
    void addsCartItemAndRedirectsBackToProductDetail() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipalFixture().build(7L);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String viewName = commerceController.addCartItem(principal, 3L, 2, redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/products/3");
        verify(commerceWorkflowService).addProductToCart(7L, 3L, 2);
    }

    @Test
    void propagatesCommerceErrorsToGlobalHandler() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipalFixture().build(7L);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        doThrow(new IllegalStateException("boom")).when(commerceWorkflowService).createPurchaseRequestDraft(7L);

        assertThatThrownBy(() -> commerceController.createPurchaseRequestDraft(principal, redirectAttributes))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }

    @Test
    void propagatesPurchaseOrderActionErrorsToGlobalHandler() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipalFixture().build(7L);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        doThrow(new IllegalStateException("forbidden"))
                .when(commerceWorkflowService)
                .submitPurchaseOrderForPlatformApproval(7L, 66L);

        assertThatThrownBy(() -> commerceController.submitPurchaseOrderForPlatformApproval(principal, 66L, redirectAttributes))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("forbidden");
    }

    @Test
    void updatesCartItemQuantityAndRedirectsToCartSection() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipalFixture().build(7L);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String viewName = commerceController.updateCartItemQuantity(principal, 31L, 5, redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/workspace?section=commerce&commerceSection=cart");
        verify(commerceWorkflowService).updateCartItemQuantity(7L, 31L, 5);
    }

    @Test
    void removesCartItemAndRedirectsToCartSection() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipalFixture().build(7L);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String viewName = commerceController.removeCartItem(principal, 31L, redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/workspace?section=commerce&commerceSection=cart");
        verify(commerceWorkflowService).removeCartItem(7L, 31L);
    }

    @Test
    void clearsCartAndRedirectsToCartSection() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipalFixture().build(7L);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String viewName = commerceController.clearCart(principal, redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/workspace?section=commerce&commerceSection=cart");
        verify(commerceWorkflowService).clearCart(7L);
    }

    @Test
    void createsPurchaseRequestDraftAndRedirectsToPurchaseRequestsSection() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipalFixture().build(7L);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String viewName = commerceController.createPurchaseRequestDraft(principal, redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/workspace?section=commerce&commerceSection=purchase-requests");
        verify(commerceWorkflowService).createPurchaseRequestDraft(7L);
    }

    @Test
    void submitsPurchaseRequestAndRedirectsToPurchaseRequestsSection() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipalFixture().build(7L);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String viewName = commerceController.submitPurchaseRequest(principal, 44L, redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/workspace?section=commerce&commerceSection=purchase-requests");
        verify(commerceWorkflowService).submitPurchaseRequest(7L, 44L);
    }

    @Test
    void cancelsPurchaseRequestAndRedirectsToPurchaseRequestsSection() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipalFixture().build(7L);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String viewName = commerceController.cancelPurchaseRequest(principal, 44L, redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/workspace?section=commerce&commerceSection=purchase-requests");
        verify(commerceWorkflowService).cancelPurchaseRequest(7L, 44L);
    }

    @Test
    void approvesApprovalAndRedirectsToApprovalsSection() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipalFixture().build(7L);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String viewName = commerceController.approveApproval(principal, 55L, null, redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/workspace?section=commerce&commerceSection=approvals");
        verify(commerceWorkflowService).approveApproval(7L, 55L, null);
    }

    @Test
    void rejectsApprovalAndRedirectsToApprovalsSection() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipalFixture().build(7L);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String viewName = commerceController.rejectApproval(principal, 55L, "budget exceeded", redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/workspace?section=commerce&commerceSection=approvals");
        verify(commerceWorkflowService).rejectApproval(7L, 55L, "budget exceeded");
    }

    @Test
    void submitsPurchaseOrderForPlatformApprovalAndRedirectsToOrderDetail() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipalFixture().build(7L);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String viewName = commerceController.submitPurchaseOrderForPlatformApproval(principal, 66L, redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/workspace?section=commerce&commerceSection=order-drafts&orderId=66");
        assertThat(redirectAttributes.getFlashAttributes().get("commerceMessage")).isEqualTo("주문 초안을 플랫폼 승인 대기로 전환했습니다.");
        verify(commerceWorkflowService).submitPurchaseOrderForPlatformApproval(7L, 66L);
    }

    @Test
    void cancelsPurchaseOrderAndRedirectsToOrderDetail() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipalFixture().build(7L);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String viewName = commerceController.cancelPurchaseOrder(principal, 66L, redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/workspace?section=commerce&commerceSection=order-drafts&orderId=66");
        assertThat(redirectAttributes.getFlashAttributes().get("commerceMessage")).isEqualTo("주문 초안을 취소했습니다.");
        verify(commerceWorkflowService).cancelPurchaseOrder(7L, 66L);
    }

    @Test
    void paysPurchaseOrderAndRedirectsToOrderDetail() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipalFixture().build(7L);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String viewName = commerceController.payPurchaseOrder(principal, 66L, redirectAttributes);

        assertThat(viewName).isEqualTo("redirect:/workspace?section=commerce&commerceSection=order-drafts&orderId=66");
        assertThat(redirectAttributes.getFlashAttributes().get("commerceAlert")).isEqualTo("결제를 완료했습니다.");
        verify(commerceWorkflowService).payPurchaseOrder(7L, 66L);
    }

    private static final class AuthenticatedUserPrincipalFixture {
        AuthenticatedUserPrincipal build(Long userId) {
            User user = User.createOAuthUser(
                    Company.builder().companyName("Example").status("ACTIVE").build(),
                    Role.builder().roleName(RoleNames.PURCHASER).description("purchaser").build(),
                    "user@example.com",
                    "User",
                    null
            );
            ReflectionTestUtils.setField(user, "id", userId);
            return AuthenticatedUserPrincipal.from(user, java.util.List.of());
        }
    }
}
