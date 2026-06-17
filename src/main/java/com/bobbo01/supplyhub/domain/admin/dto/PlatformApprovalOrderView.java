package com.bobbo01.supplyhub.domain.admin.dto;

import com.bobbo01.supplyhub.domain.purchase.dto.PurchaseOrderStatusHistoryView;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PlatformApprovalOrderView(
        Long purchaseOrderId,
        Long purchaseRequestId,
        Long companyId,
        String companyName,
        String buyerName,
        String buyerEmail,
        int itemCount,
        BigDecimal totalAmount,
        String status,
        String statusLabel,
        LocalDateTime submittedForPlatformApprovalAt,
        LocalDateTime platformReviewedAt,
        LocalDateTime paidAt,
        LocalDateTime readyToShipAt,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        String platformReviewerName,
        String platformReviewMemo,
        String platformRejectionReason,
        boolean canApprove,
        boolean canReject,
        boolean canMarkReadyToShip,
        boolean canMarkShipped,
        boolean canMarkDelivered,
        boolean hasAvailableAction,
        String actionGuideMessage,
        List<PurchaseOrderStatusHistoryView> statusHistory
) {
}
