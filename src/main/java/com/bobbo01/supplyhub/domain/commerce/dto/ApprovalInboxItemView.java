package com.bobbo01.supplyhub.domain.commerce.dto;

import java.math.BigDecimal;

public record ApprovalInboxItemView(
        Long approvalRequestId,
        Long purchaseRequestId,
        String requesterName,
        String requesterEmail,
        int itemCount,
        BigDecimal totalAmount,
        java.util.List<PurchaseRequestItemView> items
) {
}
