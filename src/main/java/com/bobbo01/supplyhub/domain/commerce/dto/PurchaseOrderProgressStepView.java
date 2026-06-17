package com.bobbo01.supplyhub.domain.commerce.dto;

public record PurchaseOrderProgressStepView(
        String code,
        String label,
        boolean completed,
        boolean current
) {
}
