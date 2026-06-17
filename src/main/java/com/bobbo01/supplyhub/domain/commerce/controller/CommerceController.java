package com.bobbo01.supplyhub.domain.commerce.controller;

import com.bobbo01.supplyhub.domain.commerce.service.CommerceWorkflowService;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/commerce")
public class CommerceController {

    private final CommerceWorkflowService commerceWorkflowService;

    public CommerceController(CommerceWorkflowService commerceWorkflowService) {
        this.commerceWorkflowService = commerceWorkflowService;
    }

    @PostMapping("/cart/items")
    public String addCartItem(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("productId") Long productId,
            @RequestParam("quantity") Integer quantity,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        commerceWorkflowService.addProductToCart(principal.getUserId(), productId, quantity);
        redirectAttributes.addFlashAttribute("commerceMessage", "장바구니에 상품을 담았습니다.");
        return "redirect:/products/" + productId;
    }

    @PostMapping("/cart/items/quantity")
    public String updateCartItemQuantity(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("cartItemId") Long cartItemId,
            @RequestParam("quantity") Integer quantity,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        commerceWorkflowService.updateCartItemQuantity(principal.getUserId(), cartItemId, quantity);
        redirectAttributes.addFlashAttribute("commerceMessage", "장바구니 수량을 변경했습니다.");
        return "redirect:/workspace?section=commerce&commerceSection=cart";
    }

    @PostMapping("/cart/items/remove")
    public String removeCartItem(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("cartItemId") Long cartItemId,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        commerceWorkflowService.removeCartItem(principal.getUserId(), cartItemId);
        redirectAttributes.addFlashAttribute("commerceMessage", "장바구니에서 상품을 제거했습니다.");
        return "redirect:/workspace?section=commerce&commerceSection=cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        commerceWorkflowService.clearCart(principal.getUserId());
        redirectAttributes.addFlashAttribute("commerceMessage", "장바구니를 비웠습니다.");
        return "redirect:/workspace?section=commerce&commerceSection=cart";
    }

    @PostMapping("/purchase-requests/draft")
    public String createPurchaseRequestDraft(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        commerceWorkflowService.createPurchaseRequestDraft(principal.getUserId());
        redirectAttributes.addFlashAttribute("commerceMessage", "구매 요청 초안을 만들었습니다.");
        return "redirect:/workspace?section=commerce&commerceSection=purchase-requests";
    }

    @PostMapping("/purchase-requests/submit")
    public String submitPurchaseRequest(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("purchaseRequestId") Long purchaseRequestId,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        commerceWorkflowService.submitPurchaseRequest(principal.getUserId(), purchaseRequestId);
        redirectAttributes.addFlashAttribute("commerceMessage", "구매 요청을 제출하고 승인 단계로 보냈습니다.");
        return "redirect:/workspace?section=commerce&commerceSection=purchase-requests";
    }

    @PostMapping("/purchase-requests/cancel")
    public String cancelPurchaseRequest(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("purchaseRequestId") Long purchaseRequestId,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        commerceWorkflowService.cancelPurchaseRequest(principal.getUserId(), purchaseRequestId);
        redirectAttributes.addFlashAttribute("commerceMessage", "구매 요청 초안을 취소했습니다.");
        return "redirect:/workspace?section=commerce&commerceSection=purchase-requests";
    }

    @PostMapping("/approval-requests/approve")
    public String approveApproval(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("approvalRequestId") Long approvalRequestId,
            @RequestParam(name = "decisionNote", required = false) String decisionNote,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        commerceWorkflowService.approveApproval(principal.getUserId(), approvalRequestId, decisionNote);
        redirectAttributes.addFlashAttribute("commerceMessage", "승인을 완료하고 주문 초안을 생성했습니다.");
        return "redirect:/workspace?section=commerce&commerceSection=approvals";
    }

    @PostMapping("/approval-requests/reject")
    public String rejectApproval(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("approvalRequestId") Long approvalRequestId,
            @RequestParam("decisionNote") String decisionNote,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        commerceWorkflowService.rejectApproval(principal.getUserId(), approvalRequestId, decisionNote);
        redirectAttributes.addFlashAttribute("commerceMessage", "구매 요청을 반려했습니다.");
        return "redirect:/workspace?section=commerce&commerceSection=approvals";
    }

    @PostMapping("/purchase-orders/submit-for-platform-approval")
    public String submitPurchaseOrderForPlatformApproval(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("purchaseOrderId") Long purchaseOrderId,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        commerceWorkflowService.submitPurchaseOrderForPlatformApproval(principal.getUserId(), purchaseOrderId);
        redirectAttributes.addFlashAttribute("commerceMessage", "주문 초안을 플랫폼 승인 대기로 전환했습니다.");
        return redirectToPurchaseOrderDraft(purchaseOrderId);
    }

    @PostMapping("/purchase-orders/cancel")
    public String cancelPurchaseOrder(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("purchaseOrderId") Long purchaseOrderId,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        commerceWorkflowService.cancelPurchaseOrder(principal.getUserId(), purchaseOrderId);
        redirectAttributes.addFlashAttribute("commerceMessage", "주문 초안을 취소했습니다.");
        return redirectToPurchaseOrderDraft(purchaseOrderId);
    }

    @PostMapping("/purchase-orders/pay")
    public String payPurchaseOrder(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("purchaseOrderId") Long purchaseOrderId,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        commerceWorkflowService.payPurchaseOrder(principal.getUserId(), purchaseOrderId);
        redirectAttributes.addFlashAttribute("commerceAlert", "결제를 완료했습니다.");
        return redirectToPurchaseOrderDraft(purchaseOrderId);
    }

    private String redirectToPurchaseOrderDraft(Long purchaseOrderId) {
        return "redirect:/workspace?section=commerce&commerceSection=order-drafts&orderId="
                + purchaseOrderId;
    }
}
