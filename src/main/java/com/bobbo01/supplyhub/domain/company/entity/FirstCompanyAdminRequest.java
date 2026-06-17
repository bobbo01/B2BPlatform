package com.bobbo01.supplyhub.domain.company.entity;

import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.global.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "first_company_admin_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FirstCompanyAdminRequest extends BaseEntity {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "first_company_admin_request_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Builder
    public FirstCompanyAdminRequest(
            Company company,
            User requester,
            User reviewedBy,
            String status,
            String reviewNote,
            LocalDateTime reviewedAt
    ) {
        this.company = company;
        this.requester = requester;
        this.reviewedBy = reviewedBy;
        this.status = status;
        this.reviewNote = reviewNote;
        this.reviewedAt = reviewedAt;
    }

    public static FirstCompanyAdminRequest createPending(Company company, User requester) {
        return FirstCompanyAdminRequest.builder()
                .company(company)
                .requester(requester)
                .status(STATUS_PENDING)
                .build();
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    public void approve(User reviewer, String reviewNote) {
        this.reviewedBy = reviewer;
        this.reviewNote = reviewNote;
        this.reviewedAt = LocalDateTime.now();
        this.status = STATUS_APPROVED;
    }

    public void reject(User reviewer, String reviewNote) {
        this.reviewedBy = reviewer;
        this.reviewNote = reviewNote;
        this.reviewedAt = LocalDateTime.now();
        this.status = STATUS_REJECTED;
    }

    public void cancel() {
        this.reviewedBy = null;
        this.reviewNote = null;
        this.reviewedAt = LocalDateTime.now();
        this.status = STATUS_CANCELLED;
    }
}
