package com.bobbo01.supplyhub.domain.company.entity;

import com.bobbo01.supplyhub.global.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "company_registration_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyRegistrationRequest extends BaseEntity {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_registration_request_id")
    private Long id;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified;

    @Column(name = "requester_name", nullable = false, length = 100)
    private String requesterName;

    @Column(name = "requester_phone", length = 50)
    private String requesterPhone;

    @Column(name = "requested_company_name", nullable = false, length = 120)
    private String requestedCompanyName;

    @Column(name = "requested_company_domain", nullable = false, length = 120)
    private String requestedCompanyDomain;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_memo", length = 500)
    private String reviewMemo;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Builder
    public CompanyRegistrationRequest(
            String provider,
            String providerUserId,
            String email,
            Boolean emailVerified,
            String requesterName,
            String requesterPhone,
            String requestedCompanyName,
            String requestedCompanyDomain,
            String status,
            Long reviewedByUserId,
            LocalDateTime reviewedAt,
            String reviewMemo,
            String rejectionReason
    ) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.emailVerified = emailVerified;
        this.requesterName = requesterName;
        this.requesterPhone = requesterPhone;
        this.requestedCompanyName = requestedCompanyName;
        this.requestedCompanyDomain = requestedCompanyDomain;
        this.status = status;
        this.reviewedByUserId = reviewedByUserId;
        this.reviewedAt = reviewedAt;
        this.reviewMemo = reviewMemo;
        this.rejectionReason = rejectionReason;
    }

    public static CompanyRegistrationRequest createPending(
            String provider,
            String providerUserId,
            String email,
            boolean emailVerified,
            String requesterName,
            String requesterPhone,
            String requestedCompanyName,
            String requestedCompanyDomain
    ) {
        return CompanyRegistrationRequest.builder()
                .provider(provider)
                .providerUserId(providerUserId)
                .email(email)
                .emailVerified(emailVerified)
                .requesterName(requesterName)
                .requesterPhone(requesterPhone)
                .requestedCompanyName(requestedCompanyName)
                .requestedCompanyDomain(requestedCompanyDomain)
                .status(STATUS_PENDING)
                .build();
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    public void approve(Long reviewerUserId, String reviewMemo) {
        assertPending();
        this.status = STATUS_APPROVED;
        this.reviewedByUserId = reviewerUserId;
        this.reviewedAt = LocalDateTime.now();
        this.reviewMemo = reviewMemo;
        this.rejectionReason = null;
    }

    public void reject(Long reviewerUserId, String reviewMemo, String rejectionReason) {
        assertPending();
        this.status = STATUS_REJECTED;
        this.reviewedByUserId = reviewerUserId;
        this.reviewedAt = LocalDateTime.now();
        this.reviewMemo = reviewMemo;
        this.rejectionReason = rejectionReason;
    }

    private void assertPending() {
        if (!isPending()) {
            throw new IllegalStateException("Only pending company registration requests can change state.");
        }
    }
}
