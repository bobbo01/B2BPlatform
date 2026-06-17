package com.bobbo01.supplyhub.domain.commerce.controller;

import com.bobbo01.supplyhub.domain.commerce.dto.CommerceActionResponse;
import com.bobbo01.supplyhub.domain.commerce.dto.PurchaseOrderDetailView;
import com.bobbo01.supplyhub.domain.commerce.service.CommerceWorkflowService;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/commerce")
public class CommerceApiController {

    private final CommerceWorkflowService commerceWorkflowService;

    public CommerceApiController(CommerceWorkflowService commerceWorkflowService) {
        this.commerceWorkflowService = commerceWorkflowService;
    }

    @GetMapping("/purchase-orders/{purchaseOrderId}")
    public PurchaseOrderDetailView purchaseOrderDetail(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable("purchaseOrderId") Long purchaseOrderId
    ) {
        if (principal == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        return commerceWorkflowService.getPurchaseOrderDetail(principal.getUserId(), purchaseOrderId);
    }

    @PostMapping("/purchase-orders/{purchaseOrderId}/submit-for-platform-approval")
    public ResponseEntity<CommerceActionResponse> submitPurchaseOrderForPlatformApproval(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable("purchaseOrderId") Long purchaseOrderId
    ) {
        if (principal == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        commerceWorkflowService.submitPurchaseOrderForPlatformApproval(principal.getUserId(), purchaseOrderId);
        return ResponseEntity.ok(new CommerceActionResponse(
                true,
                "주문 초안을 플랫폼 승인 대기로 전환했습니다.",
                purchaseOrderId
        ));
    }

    @PostMapping("/purchase-orders/{purchaseOrderId}/cancel")
    public ResponseEntity<CommerceActionResponse> cancelPurchaseOrder(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable("purchaseOrderId") Long purchaseOrderId
    ) {
        if (principal == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        commerceWorkflowService.cancelPurchaseOrder(principal.getUserId(), purchaseOrderId);
        return ResponseEntity.ok(new CommerceActionResponse(
                true,
                "주문 초안을 취소했습니다.",
                purchaseOrderId
        ));
    }

    @PostMapping("/purchase-orders/{purchaseOrderId}/pay")
    public ResponseEntity<CommerceActionResponse> payPurchaseOrder(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable("purchaseOrderId") Long purchaseOrderId
    ) {
        if (principal == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        commerceWorkflowService.payPurchaseOrder(principal.getUserId(), purchaseOrderId);
        return ResponseEntity.ok(new CommerceActionResponse(
                true,
                "결제를 완료했습니다.",
                purchaseOrderId
        ));
    }
}
