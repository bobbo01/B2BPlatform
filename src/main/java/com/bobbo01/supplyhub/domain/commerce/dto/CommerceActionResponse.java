package com.bobbo01.supplyhub.domain.commerce.dto;

public record CommerceActionResponse(
        boolean success,
        String message,
        Long purchaseOrderId
) {
}
