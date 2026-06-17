package com.bobbo01.supplyhub.domain.company.entity;

import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
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
@Table(name = "company_join_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyJoinRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_join_request_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CompanyJoinRequestStatus status;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    @Column(name = "requested_email", nullable = false, length = 255)
    private String requestedEmail;

    @Column(name = "requested_name", nullable = false, length = 100)
    private String requestedName;

    @Column(name = "requested_role_name", nullable = false, length = 50)
    private String requestedRoleName;

    @Column(name = "review_memo", length = 500)
    private String reviewMemo;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Builder
    public CompanyJoinRequest(
            Company company,
            User reviewedBy,
            CompanyJoinRequestStatus status,
            String provider,
            String providerUserId,
            String requestedEmail,
            String requestedName,
            String requestedRoleName,
            String reviewMemo,
            String rejectionReason,
            LocalDateTime reviewedAt
    ) {
        this.company = company;
        this.reviewedBy = reviewedBy;
        this.status = status;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.requestedEmail = requestedEmail;
        this.requestedName = requestedName;
        this.requestedRoleName = requestedRoleName;
        this.reviewMemo = reviewMemo;
        this.rejectionReason = rejectionReason;
        this.reviewedAt = reviewedAt;
    }

    public static CompanyJoinRequest createPending(
            Company company,
            String provider,
            String providerUserId,
            String requestedEmail,
            String requestedName
    ) {
        return CompanyJoinRequest.builder()
                .company(company)
                .status(CompanyJoinRequestStatus.PENDING)
                .provider(provider)
                .providerUserId(providerUserId)
                .requestedEmail(requestedEmail)
                .requestedName(requestedName)
                .requestedRoleName(RoleNames.CART_USER)
                .build();
    }

    public boolean isPending() {
        return status == CompanyJoinRequestStatus.PENDING;
    }

    public void approve(User reviewer, String reviewMemo) {
        assertPending();
        this.reviewedBy = reviewer;
        this.reviewMemo = reviewMemo;
        this.rejectionReason = null;
        this.reviewedAt = LocalDateTime.now();
        this.status = CompanyJoinRequestStatus.APPROVED;
    }

    public void reject(User reviewer, String reviewMemo, String rejectionReason) {
        assertPending();
        this.reviewedBy = reviewer;
        this.reviewMemo = reviewMemo;
        this.rejectionReason = rejectionReason;
        this.reviewedAt = LocalDateTime.now();
        this.status = CompanyJoinRequestStatus.REJECTED;
    }

    public void cancel() {
        assertPending();
        this.reviewedBy = null;
        this.reviewMemo = null;
        this.rejectionReason = null;
        this.reviewedAt = LocalDateTime.now();
        this.status = CompanyJoinRequestStatus.CANCELLED;
    }

    private void assertPending() {
        if (!isPending()) {
            throw new IllegalStateException("Only pending company join requests can change state.");
        }
    }
}
