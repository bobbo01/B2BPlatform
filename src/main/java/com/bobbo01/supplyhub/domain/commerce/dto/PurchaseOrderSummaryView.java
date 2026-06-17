package com.bobbo01.supplyhub.domain.commerce.dto;

import java.math.BigDecimal;

public record PurchaseOrderSummaryView(
        Long purchaseOrderId,
        Long purchaseRequestId,
        String statusCode,
        String statusLabel,
        int itemCount,
        BigDecimal totalAmount
) {
}
