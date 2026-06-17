package com.bobbo01.supplyhub.domain.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlatformSettlementOrderView(
        Long purchaseOrderId,
        Long purchaseRequestId,
        Long companyId,
        String companyName,
        String buyerName,
        String buyerEmail,
        BigDecimal totalAmount,
        String orderStatus,
        String orderStatusLabel,
        String settlementStatus,
        String settlementStatusLabel,
        LocalDateTime deliveredAt,
        LocalDateTime settledAt,
        String settledByName
) {
}
