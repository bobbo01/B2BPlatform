package com.bobbo01.supplyhub.domain.purchase.entity;

import com.bobbo01.supplyhub.domain.cart.entity.Cart;
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

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "purchase_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_request_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_cart_id")
    private Cart sourceCart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PurchaseRequestStatus status;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Builder
    public PurchaseRequest(
            Company company,
            User requester,
            Cart sourceCart,
            PurchaseRequestStatus status,
            LocalDateTime submittedAt,
            LocalDateTime approvedAt,
            LocalDateTime rejectedAt,
            LocalDateTime cancelledAt
    ) {
        this.company = company;
        this.requester = requester;
        this.sourceCart = sourceCart;
        this.status = status;
        this.submittedAt = submittedAt;
        this.approvedAt = approvedAt;
        this.rejectedAt = rejectedAt;
        this.cancelledAt = cancelledAt;
    }

    public static PurchaseRequest createDraft(Company company, User requester, Cart sourceCart) {
        return PurchaseRequest.builder()
                .company(company)
                .requester(requester)
                .sourceCart(sourceCart)
                .status(PurchaseRequestStatus.DRAFT)
                .build();
    }

    public void submit() {
        assertStatus(PurchaseRequestStatus.DRAFT);
        this.status = PurchaseRequestStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
    }

    public void approve() {
        assertStatus(PurchaseRequestStatus.SUBMITTED);
        this.status = PurchaseRequestStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject() {
        assertStatus(PurchaseRequestStatus.SUBMITTED);
        this.status = PurchaseRequestStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
    }

    public void cancel() {
        assertStatus(PurchaseRequestStatus.DRAFT);
        this.status = PurchaseRequestStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    private void assertStatus(PurchaseRequestStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new IllegalStateException("Purchase request is not in the required state: " + expectedStatus);
        }
    }
}
