package com.bobbo01.supplyhub.domain.commerce.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartSummaryView(
        Long cartId,
        int itemCount,
        BigDecimal totalAmount,
        List<CartItemView> items
) {
}
