package com.bobbo01.supplyhub.domain.admin.dto;

import java.math.BigDecimal;

public record PlatformSettlementSummaryView(
        BigDecimal totalOrderSales,
        BigDecimal unsettledSales,
        BigDecimal settledSales,
        long totalOrderCount,
        long unsettledOrderCount,
        long settledOrderCount
) {
}
