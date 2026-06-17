package com.bobbo01.supplyhub.domain.company.service;

import com.bobbo01.supplyhub.domain.company.dto.CompanyRegistrationRequestView;
import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.entity.CompanyRegistrationRequest;
import com.bobbo01.supplyhub.domain.company.repository.CompanyRegistrationRequestRepository;
import com.bobbo01.supplyhub.domain.company.repository.CompanyRepository;
import com.bobbo01.supplyhub.domain.company.workflow.FirstLoginCompanySetupState;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.role.repository.RoleRepository;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.entity.UserIdentity;
import com.bobbo01.supplyhub.domain.user.repository.UserIdentityRepository;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CompanyRegistrationRequestService {

    private final CompanyRegistrationRequestRepository companyRegistrationRequestRepository;
    private final CompanyRepository companyRepository;
    private final CompanyInviteCodeService companyInviteCodeService;
    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final RoleRepository roleRepository;

    public CompanyRegistrationRequestService(
            CompanyRegistrationRequestRepository companyRegistrationRequestRepository,
            CompanyRepository companyRepository,
            CompanyInviteCodeService companyInviteCodeService,
            UserRepository userRepository,
            UserIdentityRepository userIdentityRepository,
            RoleRepository roleRepository
    ) {
        this.companyRegistrationRequestRepository = companyRegistrationRequestRepository;
        this.companyRepository = companyRepository;
        this.companyInviteCodeService = companyInviteCodeService;
        this.userRepository = userRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public CompanyRegistrationRequest submitRequest(FirstLoginCompanySetupState state, String companyName, String companyDomain) {
        assertNoPendingRequest(state.registrationId(), state.providerUserId(), companyDomain);
        if (companyRepository.existsByCompanyDomainIgnoreCase(companyDomain)) {
            throw new IllegalArgumentException("Company domain is already in use.");
        }

        CompanyRegistrationRequest request = CompanyRegistrationRequest.createPending(
                state.registrationId(),
                state.providerUserId(),
                state.email(),
                state.emailVerified(),
                state.resolvedName(),
                state.phone(),
                companyName,
                companyDomain
        );
        return companyRegistrationRequestRepository.save(request);
    }

    public Optional<CompanyRegistrationRequestView> findLatestRequest(String provider, String providerUserId) {
        return companyRegistrationRequestRepository
                .findFirstByProviderAndProviderUserIdOrderByCreatedAtDesc(provider, providerUserId)
                .map(this::toPendingSetupView);
    }

    public List<CompanyRegistrationRequestView> getPendingRequestsForPlatformAdmin(Long reviewerUserId) {
        User reviewer = getRequiredUser(reviewerUserId);
        assertPlatformAdmin(reviewer);
        return companyRegistrationRequestRepository.findAllByStatusOrderByCreatedAtAsc(CompanyRegistrationRequest.STATUS_PENDING)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public void approveRequest(Long reviewerUserId, Long requestId, String reviewMemo) {
        User reviewer = getRequiredUser(reviewerUserId);
        assertPlatformAdmin(reviewer);

        CompanyRegistrationRequest request = getPendingRequest(requestId);
        if (companyRepository.existsByCompanyDomainIgnoreCase(request.getRequestedCompanyDomain())) {
            throw new IllegalStateException("?대? ?ъ슜以묒씤 ?꾨찓?몄엯?덈떎.");
        }

        Role defaultRole = roleRepository.findByRoleNameIgnoreCase(RoleNames.CART_USER)
                .orElseThrow(() -> new IllegalStateException("Default CART_USER role was not found."));

        Company company = companyRepository.save(Company.builder()
                .companyName(request.getRequestedCompanyName())
                .companyDomain(request.getRequestedCompanyDomain())
                .inviteCode(companyInviteCodeService.generateUniqueInviteCode())
                .status("ACTIVE")
                .build());

        User user = resolveOrCreateRegistrationUser(company, defaultRole, request);
        company.assignCreatorUser(user.getId());
        companyRepository.save(company);
        attachOrCreateIdentity(user, request);
        request.approve(reviewerUserId, normalizeOptionalText(reviewMemo));
    }

    @Transactional
    public void rejectRequest(Long reviewerUserId, Long requestId, String reviewMemo, String rejectionReason) {
        User reviewer = getRequiredUser(reviewerUserId);
        assertPlatformAdmin(reviewer);
        getPendingRequest(requestId).reject(
                reviewerUserId,
                normalizeOptionalText(reviewMemo),
                requireRejectionReason(rejectionReason)
        );
    }

    private void assertNoPendingRequest(String provider, String providerUserId, String companyDomain) {
        if (companyRegistrationRequestRepository.findByProviderAndProviderUserIdAndStatus(
                provider,
                providerUserId,
                CompanyRegistrationRequest.STATUS_PENDING
        ).isPresent()) {
            throw new IllegalStateException("A pending company registration request already exists for this identity.");
        }
        if (companyRegistrationRequestRepository.findByRequestedCompanyDomainIgnoreCaseAndStatus(
                companyDomain,
                CompanyRegistrationRequest.STATUS_PENDING
        ).isPresent()) {
            throw new IllegalStateException("A pending company registration request already exists for this company domain.");
        }
    }

    private CompanyRegistrationRequest getPendingRequest(Long requestId) {
        CompanyRegistrationRequest request = companyRegistrationRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Company registration request was not found."));
        if (!request.isPending()) {
            throw new IllegalStateException("Only pending company registration requests can be reviewed.");
        }
        return request;
    }

    private User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User was not found."));
    }

    private void assertPlatformAdmin(User reviewer) {
        if (!reviewer.isPlatformAdmin()) {
            throw new IllegalStateException("Only PLATFORM_ADMIN can review company registration requests.");
        }
    }

    private CompanyRegistrationRequestView toView(CompanyRegistrationRequest request) {
        return new CompanyRegistrationRequestView(
                request.getId(),
                request.getRequesterName(),
                request.getEmail(),
                request.getRequestedCompanyName(),
                request.getRequestedCompanyDomain(),
                request.getStatus(),
                request.getReviewMemo(),
                request.getRejectionReason()
        );
    }

    private CompanyRegistrationRequestView toPendingSetupView(CompanyRegistrationRequest request) {
        return new CompanyRegistrationRequestView(
                request.getId(),
                request.getRequesterName(),
                request.getEmail(),
                request.getRequestedCompanyName(),
                request.getRequestedCompanyDomain(),
                request.getStatus(),
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

    private User resolveOrCreateRegistrationUser(
            Company company,
            Role defaultRole,
            CompanyRegistrationRequest request
    ) {
        Optional<UserIdentity> existingIdentity = userIdentityRepository.findByProviderAndProviderUserId(
                request.getProvider(),
                request.getProviderUserId()
        );
        Optional<User> existingEmailUser = userRepository.findByEmailIgnoreCase(request.getEmail());

        if (existingIdentity.isPresent() && existingEmailUser.isPresent()
                && !existingIdentity.get().getUser().getId().equals(existingEmailUser.get().getId())) {
            throw new IllegalStateException("Conflicting user records already exist for this registration request.");
        }

        if (existingIdentity.isPresent()) {
            return attachDetachedUser(company, defaultRole, existingIdentity.get().getUser(), request);
        }

        if (existingEmailUser.isPresent()) {
            return attachDetachedUser(company, defaultRole, existingEmailUser.get(), request);
        }

        return userRepository.save(User.createOAuthUser(
                company,
                defaultRole,
                request.getEmail(),
                request.getRequesterName(),
                request.getRequesterPhone()
        ));
    }

    private User attachDetachedUser(
            Company company,
            Role defaultRole,
            User user,
            CompanyRegistrationRequest request
    ) {
        if (user.isPlatformAdmin()) {
            throw new IllegalStateException("A platform admin account cannot be attached to a company registration.");
        }
        if (user.getCompany() != null) {
            throw new IllegalStateException("A linked company user already exists for this registration request.");
        }
        if (!user.isActive()) {
            throw new IllegalStateException("Only active detached users can be attached to a new company.");
        }
        user.attachToCompany(company, defaultRole);
        user.updateProfile(request.getRequesterName(), request.getRequesterPhone());
        return user;
    }

    private void attachOrCreateIdentity(User user, CompanyRegistrationRequest request) {
        userIdentityRepository.findByProviderAndProviderUserId(request.getProvider(), request.getProviderUserId())
                .ifPresentOrElse(
                        identity -> identity.attachTo(user),
                        () -> userIdentityRepository.save(UserIdentity.create(
                                user,
                                request.getProvider(),
                                request.getProviderUserId(),
                                request.getEmail(),
                                Boolean.TRUE.equals(request.getEmailVerified())
                        ))
                );
    }
}
