package com.bobbo01.supplyhub.domain.company.service;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.dto.CompanyJoinRequestView;
import com.bobbo01.supplyhub.domain.company.entity.CompanyJoinRequest;
import com.bobbo01.supplyhub.domain.company.entity.CompanyJoinRequestStatus;
import com.bobbo01.supplyhub.domain.company.repository.CompanyJoinRequestRepository;
import com.bobbo01.supplyhub.domain.role.RoleLabeler;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import com.bobbo01.supplyhub.domain.user.service.UserOAuthAccountService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyJoinRequestService {

    private final CompanyJoinRequestRepository companyJoinRequestRepository;
    private final UserRepository userRepository;
    private final UserOAuthAccountService userOAuthAccountService;

    public CompanyJoinRequestService(
            CompanyJoinRequestRepository companyJoinRequestRepository,
            UserRepository userRepository,
            UserOAuthAccountService userOAuthAccountService
    ) {
        this.companyJoinRequestRepository = companyJoinRequestRepository;
        this.userRepository = userRepository;
        this.userOAuthAccountService = userOAuthAccountService;
    }

    @Transactional
    public CompanyJoinRequest submitJoinRequest(
            Company company,
            String provider,
            String providerUserId,
            String requestedEmail,
            String requestedName
    ) {
        if (!company.isActive()) {
            throw new IllegalStateException("Join requests are only allowed for active companies.");
        }
        if (companyJoinRequestRepository.existsByProviderAndProviderUserIdAndStatus(
                provider,
                providerUserId,
                CompanyJoinRequestStatus.PENDING
        )) {
            throw new IllegalStateException("A pending join request already exists for this identity.");
        }
        if (companyJoinRequestRepository.existsByCompanyIdAndRequestedEmailIgnoreCaseAndStatus(
                company.getId(),
                requestedEmail,
                CompanyJoinRequestStatus.PENDING
        )) {
            throw new IllegalStateException("A pending join request already exists for this company email.");
        }

        CompanyJoinRequest request = CompanyJoinRequest.createPending(
                company,
                provider,
                providerUserId,
                requestedEmail,
                requestedName
        );
        return companyJoinRequestRepository.save(request);
    }

    @Transactional
    public void cancelOwnPendingRequest(String provider, String providerUserId) {
        CompanyJoinRequest request = companyJoinRequestRepository
                .findFirstByProviderAndProviderUserIdOrderByCreatedAtDesc(provider, providerUserId)
                .orElseThrow(() -> new IllegalStateException("Company join request was not found."));

        request.cancel();
    }

    public java.util.Optional<CompanyJoinRequestView> findLatestRequest(String provider, String providerUserId) {
        return companyJoinRequestRepository.findFirstByProviderAndProviderUserIdOrderByCreatedAtDesc(provider, providerUserId)
                .map(this::toPendingSetupView);
    }

    @Transactional
    public void approveRequest(Long reviewerUserId, Long requestId, String reviewMemo) {
        User reviewer = getRequiredReviewer(reviewerUserId);
        CompanyJoinRequest request = getPendingRequest(requestId);
        if (request.getCompany().getId() == null || reviewer.getCompany() == null) {
            throw new IllegalStateException("Company-scoped approval requires a company reviewer.");
        }
        if (!request.getCompany().getId().equals(reviewer.getCompany().getId())) {
            throw new IllegalStateException("A company admin can only review requests for their own company.");
        }
        userOAuthAccountService.provisionApprovedCompanyJoinRequest(request);
        request.approve(reviewer, normalizeOptionalText(reviewMemo));
    }

    @Transactional
    public void rejectRequest(Long reviewerUserId, Long requestId, String reviewMemo, String rejectionReason) {
        User reviewer = getRequiredReviewer(reviewerUserId);
        CompanyJoinRequest request = getPendingRequest(requestId);
        if (request.getCompany().getId() == null || reviewer.getCompany() == null) {
            throw new IllegalStateException("Company-scoped review requires a company reviewer.");
        }
        if (!request.getCompany().getId().equals(reviewer.getCompany().getId())) {
            throw new IllegalStateException("A company admin can only review requests for their own company.");
        }
        request.reject(
                reviewer,
                normalizeOptionalText(reviewMemo),
                requireRejectionReason(rejectionReason)
        );
    }

    public List<CompanyJoinRequestView> getPendingRequestsForCompany(Long reviewerUserId) {
        User reviewer = getRequiredReviewer(reviewerUserId);
        if (reviewer.getCompany() == null) {
            throw new IllegalStateException("Company reviewer must belong to a company.");
        }
        return companyJoinRequestRepository.findAllByCompanyIdAndStatusOrderByCreatedAtAsc(
                reviewer.getCompany().getId(),
                CompanyJoinRequestStatus.PENDING
        ).stream()
                .map(this::toView)
                .toList();
    }

    private User getRequiredReviewer(Long userId) {
        User reviewer = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User was not found."));
        if (!reviewer.hasCompanyAdminRole()) {
            throw new IllegalStateException("Only COMPANY_ADMIN can review join requests.");
        }
        return reviewer;
    }

    private CompanyJoinRequest getPendingRequest(Long requestId) {
        CompanyJoinRequest request = companyJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Company join request was not found."));
        if (!request.isPending()) {
            throw new IllegalStateException("Only pending company join requests can be reviewed.");
        }
        return request;
    }

    private CompanyJoinRequestView toView(CompanyJoinRequest request) {
        return new CompanyJoinRequestView(
                request.getId(),
                request.getCompany().getCompanyName(),
                request.getCompany().getCompanyDomain(),
                request.getRequestedName(),
                request.getRequestedEmail(),
                request.getStatus().name(),
                request.getRequestedRoleName(),
                RoleLabeler.toRoleLabel(request.getRequestedRoleName()),
                request.getReviewMemo(),
                request.getRejectionReason()
        );
    }

    private CompanyJoinRequestView toPendingSetupView(CompanyJoinRequest request) {
        return new CompanyJoinRequestView(
                request.getId(),
                request.getCompany().getCompanyName(),
                request.getCompany().getCompanyDomain(),
                request.getRequestedName(),
                request.getRequestedEmail(),
                request.getStatus().name(),
                request.getRequestedRoleName(),
                RoleLabeler.toRoleLabel(request.getRequestedRoleName()),
                null,
                request.getRejectionReason()
        );
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String requireRejectionReason(String rejectionReason) {
        String normalized = normalizeOptionalText(rejectionReason);
        if (normalized == null) {
            throw new IllegalArgumentException("Rejection reason is required.");
        }
        return normalized;
    }
}
