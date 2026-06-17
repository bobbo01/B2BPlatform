package com.bobbo01.supplyhub.domain.commerce.dto;

import java.math.BigDecimal;

public record PurchaseRequestSummaryView(
        Long purchaseRequestId,
        String status,
        int itemCount,
        BigDecimal totalAmount,
        boolean canSubmit,
        boolean canCancel,
        java.util.List<PurchaseRequestItemView> items
) {
}
