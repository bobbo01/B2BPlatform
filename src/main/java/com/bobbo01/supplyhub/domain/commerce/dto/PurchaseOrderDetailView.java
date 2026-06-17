package com.bobbo01.supplyhub.domain.commerce.dto;

import com.bobbo01.supplyhub.domain.purchase.dto.PurchaseOrderStatusHistoryView;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderDetailView(
        Long purchaseOrderId,
        Long purchaseRequestId,
        String statusCode,
        String statusLabel,
        String statusGuideTitle,
        String statusGuideMessage,
        boolean hasAvailableAction,
        String actionGuideTitle,
        String actionGuideMessage,
        boolean terminalStatus,
        int itemCount,
        BigDecimal totalAmount,
        boolean canSubmitForPlatformApproval,
        boolean canCancel,
        boolean canPay,
        LocalDateTime submittedForPlatformApprovalAt,
        LocalDateTime placedAt,
        LocalDateTime paidAt,
        LocalDateTime cancelledAt,
        String platformReviewedByName,
        LocalDateTime platformReviewedAt,
        String platformReviewMemo,
        String platformRejectionReason,
        List<PurchaseOrderProgressStepView> progressSteps,
        List<PurchaseOrderStatusHistoryView> statusHistory,
        List<PurchaseOrderItemDetailView> items
) {
}
