package com.bobbo01.supplyhub.domain.purchase.entity;

public enum PurchaseOrderStatus {
    DRAFT,
    PENDING_PLATFORM_APPROVAL,
    PAYMENT_PENDING,
    PAID,
    READY_TO_SHIP,
    SHIPPED,
    DELIVERED,
    REJECTED,
    CANCELLED
}
