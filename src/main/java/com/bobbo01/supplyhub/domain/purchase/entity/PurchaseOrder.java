package com.bobbo01.supplyhub.domain.purchase.entity;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.global.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "purchase_orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_request_id", nullable = false)
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_user_id", nullable = false)
    private User buyer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PurchaseOrderStatus status;

    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;

    @Column(name = "submitted_for_platform_approval_at")
    private LocalDateTime submittedForPlatformApprovalAt;

    @Column(name = "placed_at")
    private LocalDateTime placedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "ready_to_ship_at")
    private LocalDateTime readyToShipAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", length = 20)
    private PurchaseOrderSettlementStatus settlementStatus;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settled_by_user_id")
    private User settledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_reviewed_by_user_id")
    private User platformReviewedBy;

    @Column(name = "platform_reviewed_at")
    private LocalDateTime platformReviewedAt;

    @Column(name = "platform_review_memo", length = 1000)
    private String platformReviewMemo;

    @Column(name = "platform_rejection_reason", length = 1000)
    private String platformRejectionReason;

    @Builder
    public PurchaseOrder(
            Company company,
            PurchaseRequest purchaseRequest,
            User buyer,
            PurchaseOrderStatus status,
            BigDecimal subtotalAmount,
            BigDecimal totalAmount,
            String currencyCode,
            LocalDateTime submittedForPlatformApprovalAt,
            LocalDateTime placedAt,
            LocalDateTime paidAt,
            LocalDateTime readyToShipAt,
            LocalDateTime shippedAt,
            LocalDateTime deliveredAt,
            PurchaseOrderSettlementStatus settlementStatus,
            LocalDateTime settledAt,
            User settledBy,
            LocalDateTime cancelledAt,
            User platformReviewedBy,
            LocalDateTime platformReviewedAt,
            String platformReviewMemo,
            String platformRejectionReason
    ) {
        this.company = company;
        this.purchaseRequest = purchaseRequest;
        this.buyer = buyer;
        this.status = status;
        this.subtotalAmount = subtotalAmount;
        this.totalAmount = totalAmount;
        this.currencyCode = currencyCode;
        this.submittedForPlatformApprovalAt = submittedForPlatformApprovalAt;
        this.placedAt = placedAt;
        this.paidAt = paidAt;
        this.readyToShipAt = readyToShipAt;
        this.shippedAt = shippedAt;
        this.deliveredAt = deliveredAt;
        this.settlementStatus = settlementStatus;
        this.settledAt = settledAt;
        this.settledBy = settledBy;
        this.cancelledAt = cancelledAt;
        this.platformReviewedBy = platformReviewedBy;
        this.platformReviewedAt = platformReviewedAt;
        this.platformReviewMemo = platformReviewMemo;
        this.platformRejectionReason = platformRejectionReason;
    }

    public static PurchaseOrder createDraft(Company company, PurchaseRequest purchaseRequest, User buyer) {
        return PurchaseOrder.builder()
                .company(company)
                .purchaseRequest(purchaseRequest)
                .buyer(buyer)
                .status(PurchaseOrderStatus.DRAFT)
                .subtotalAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .currencyCode("KRW")
                .settlementStatus(PurchaseOrderSettlementStatus.UNSETTLED)
                .build();
    }

    public PurchaseOrderSettlementStatus getSettlementStatus() {
        return settlementStatus != null ? settlementStatus : PurchaseOrderSettlementStatus.UNSETTLED;
    }

    public void applyPricingSnapshot(BigDecimal subtotalAmount, BigDecimal totalAmount, String currencyCode) {
        if (status != PurchaseOrderStatus.DRAFT) {
            throw new IllegalStateException("Pricing snapshot can only be set for draft orders.");
        }
        if (subtotalAmount == null || totalAmount == null) {
            throw new IllegalArgumentException("Pricing snapshot amounts are required.");
        }
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new IllegalArgumentException("Currency code is required.");
        }
        this.subtotalAmount = subtotalAmount;
        this.totalAmount = totalAmount;
        this.currencyCode = currencyCode.trim();
    }

    public void submitForPlatformApproval() {
        if (!canSubmitForPlatformApproval()) {
            throw new IllegalStateException("Purchase order cannot be submitted for platform approval.");
        }
        this.status = PurchaseOrderStatus.PENDING_PLATFORM_APPROVAL;
        this.submittedForPlatformApprovalAt = LocalDateTime.now();
        this.platformReviewedBy = null;
        this.platformReviewedAt = null;
        this.platformReviewMemo = null;
        this.platformRejectionReason = null;
    }

    public void approveByPlatform(User reviewer, String reviewMemo) {
        assertStatus(PurchaseOrderStatus.PENDING_PLATFORM_APPROVAL);
        this.status = PurchaseOrderStatus.PAYMENT_PENDING;
        this.platformReviewedBy = reviewer;
        this.platformReviewedAt = LocalDateTime.now();
        this.platformReviewMemo = reviewMemo;
        this.platformRejectionReason = null;
        this.placedAt = this.platformReviewedAt;
        this.paidAt = null;
    }

    public void rejectByPlatform(User reviewer, String reviewMemo, String rejectionReason) {
        assertStatus(PurchaseOrderStatus.PENDING_PLATFORM_APPROVAL);
        this.status = PurchaseOrderStatus.REJECTED;
        this.platformReviewedBy = reviewer;
        this.platformReviewedAt = LocalDateTime.now();
        this.platformReviewMemo = reviewMemo;
        this.platformRejectionReason = rejectionReason;
        this.placedAt = null;
    }

    public void cancel() {
        if (!canCancel()) {
            throw new IllegalStateException("Purchase order can no longer be cancelled.");
        }
        this.status = PurchaseOrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    public boolean canSubmitForPlatformApproval() {
        return status == PurchaseOrderStatus.DRAFT;
    }

    public boolean canCancel() {
        return status == PurchaseOrderStatus.DRAFT
                || status == PurchaseOrderStatus.PENDING_PLATFORM_APPROVAL;
    }

    public boolean isConfirmed() {
        return status == PurchaseOrderStatus.PAYMENT_PENDING
                || status == PurchaseOrderStatus.PAID
                || status == PurchaseOrderStatus.READY_TO_SHIP
                || status == PurchaseOrderStatus.SHIPPED
                || status == PurchaseOrderStatus.DELIVERED;
    }

    public boolean isRejected() {
        return status == PurchaseOrderStatus.REJECTED;
    }

    public boolean canPay() {
        return status == PurchaseOrderStatus.PAYMENT_PENDING;
    }

    public void markPaid() {
        if (!canPay()) {
            throw new IllegalStateException("Purchase order is not waiting for payment.");
        }
        this.status = PurchaseOrderStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    public boolean canMarkReadyToShip() {
        return status == PurchaseOrderStatus.PAID;
    }

    public void markReadyToShip() {
        if (!canMarkReadyToShip()) {
            throw new IllegalStateException("Purchase order is not ready to move to shipping preparation.");
        }
        this.status = PurchaseOrderStatus.READY_TO_SHIP;
        this.readyToShipAt = LocalDateTime.now();
    }

    public boolean canShip() {
        return status == PurchaseOrderStatus.READY_TO_SHIP;
    }

    public void markShipped() {
        if (!canShip()) {
            throw new IllegalStateException("Purchase order is not ready to be shipped.");
        }
        this.status = PurchaseOrderStatus.SHIPPED;
        this.shippedAt = LocalDateTime.now();
    }

    public boolean canDeliver() {
        return status == PurchaseOrderStatus.SHIPPED;
    }

    public void markDelivered() {
        if (!canDeliver()) {
            throw new IllegalStateException("Purchase order is not ready to be marked as delivered.");
        }
        this.status = PurchaseOrderStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    public boolean canSettle() {
        return status == PurchaseOrderStatus.DELIVERED
                && getSettlementStatus() == PurchaseOrderSettlementStatus.UNSETTLED;
    }

    public void markSettled(User settledBy) {
        if (!canSettle()) {
            throw new IllegalStateException("Purchase order is not ready to be settled.");
        }
        this.settlementStatus = PurchaseOrderSettlementStatus.SETTLED;
        this.settledAt = LocalDateTime.now();
        this.settledBy = settledBy;
    }

    private void assertStatus(PurchaseOrderStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new IllegalStateException("Purchase order is not in the required state: " + expectedStatus);
        }
    }
}
