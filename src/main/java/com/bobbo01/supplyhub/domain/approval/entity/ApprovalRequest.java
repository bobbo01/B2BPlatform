package com.bobbo01.supplyhub.domain.approval.entity;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.purchase.entity.PurchaseRequest;
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
@Table(name = "approval_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_request_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_request_id", nullable = false)
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approver_user_id", nullable = false)
    private User approver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApprovalRequestStatus status;

    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Builder
    public ApprovalRequest(
            Company company,
            PurchaseRequest purchaseRequest,
            User requester,
            User approver,
            ApprovalRequestStatus status,
            String decisionNote,
            LocalDateTime decidedAt
    ) {
        this.company = company;
        this.purchaseRequest = purchaseRequest;
        this.requester = requester;
        this.approver = approver;
        this.status = status;
        this.decisionNote = decisionNote;
        this.decidedAt = decidedAt;
    }

    public static ApprovalRequest createPending(
            Company company,
            PurchaseRequest purchaseRequest,
            User requester,
            User approver
    ) {
        return ApprovalRequest.builder()
                .company(company)
                .purchaseRequest(purchaseRequest)
                .requester(requester)
                .approver(approver)
                .status(ApprovalRequestStatus.PENDING)
                .build();
    }

    public void approve(String decisionNote) {
        assertPending();
        this.status = ApprovalRequestStatus.APPROVED;
        this.decisionNote = decisionNote;
        this.decidedAt = LocalDateTime.now();
    }

    public void reject(String decisionNote) {
        assertPending();
        this.status = ApprovalRequestStatus.REJECTED;
        this.decisionNote = decisionNote;
        this.decidedAt = LocalDateTime.now();
    }

    public void cancel(String decisionNote) {
        assertPending();
        this.status = ApprovalRequestStatus.CANCELLED;
        this.decisionNote = decisionNote;
        this.decidedAt = LocalDateTime.now();
    }

    private void assertPending() {
        if (status != ApprovalRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending approvals can change state.");
        }
    }
}
