package com.bobbo01.supplyhub.domain.company.service;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.dto.CompanyJoinRequestView;
import com.bobbo01.supplyhub.domain.company.repository.CompanyRepository;
import com.bobbo01.supplyhub.domain.company.workflow.FirstLoginCompanySetupState;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.service.UserOAuthAccountService;
import com.bobbo01.supplyhub.global.auth.oauth.OAuth2AccountService;
import com.bobbo01.supplyhub.global.auth.oauth.OAuth2Attributes;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class FirstLoginCompanySetupService {

    private static final Logger log = LoggerFactory.getLogger(FirstLoginCompanySetupService.class);
    public static final String SESSION_ATTRIBUTE = "firstLoginCompanySetupState";
    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final Pattern COMPANY_DOMAIN_PATTERN = Pattern.compile("^(?=.{1,120}$)(?!-)(?:[a-z0-9-]+\\.)+[a-z]{2,63}$");

    private final CompanyRepository companyRepository;
    private final CompanyRegistrationRequestService companyRegistrationRequestService;
    private final CompanyJoinRequestService companyJoinRequestService;
    private final CompanyInviteCodeService companyInviteCodeService;
    private final UserOAuthAccountService userOAuthAccountService;
    private final OAuth2AccountService oauth2AccountService;

    public FirstLoginCompanySetupService(
            CompanyRepository companyRepository,
            CompanyRegistrationRequestService companyRegistrationRequestService,
            CompanyJoinRequestService companyJoinRequestService,
            CompanyInviteCodeService companyInviteCodeService,
            UserOAuthAccountService userOAuthAccountService,
            OAuth2AccountService oauth2AccountService
    ) {
        this.companyRepository = companyRepository;
        this.companyRegistrationRequestService = companyRegistrationRequestService;
        this.companyJoinRequestService = companyJoinRequestService;
        this.companyInviteCodeService = companyInviteCodeService;
        this.userOAuthAccountService = userOAuthAccountService;
        this.oauth2AccountService = oauth2AccountService;
    }

    public void storePendingState(HttpSession session, OAuth2Attributes attributes) {
        Instant now = Instant.now();
        String emailDomain = oauth2AccountService.extractRequiredDomain(attributes);
        session.setAttribute(SESSION_ATTRIBUTE, new FirstLoginCompanySetupState(
                attributes.registrationId(),
                attributes.providerUserId(),
                attributes.email(),
                emailDomain,
                oauth2AccountService.loginPolicy().isPublicEmailDomain(emailDomain),
                attributes.resolvedName(),
                attributes.phone(),
                attributes.emailVerified(),
                now,
                now.plus(STATE_TTL),
                false
        ));
    }

    public FirstLoginCompanySetupState getRequiredState(HttpSession session) {
        Object value = session.getAttribute(SESSION_ATTRIBUTE);
        if (!(value instanceof FirstLoginCompanySetupState state)) {
            log.warn("Pending company setup state missing: sessionId={}", session.getId());
            throw new IllegalStateException("First login company setup state is missing.");
        }
        if (state.consumed() || state.isExpired(Instant.now())) {
            log.warn("Pending company setup state expired or consumed: sessionId={}, consumed={}, expiresAt={}",
                    session.getId(), state.consumed(), state.expiresAt());
            clearPendingState(session);
            throw new IllegalStateException("First login company setup state has expired.");
        }
        return state;
    }

    @Transactional(readOnly = true)
    public Optional<com.bobbo01.supplyhub.domain.company.dto.CompanyRegistrationRequestView> findLatestRegistrationRequest(HttpSession session) {
        FirstLoginCompanySetupState state = getRequiredState(session);
        return companyRegistrationRequestService.findLatestRequest(state.registrationId(), state.providerUserId());
    }

    @Transactional(readOnly = true)
    public Optional<CompanyJoinRequestView> findLatestJoinRequest(HttpSession session) {
        FirstLoginCompanySetupState state = getRequiredState(session);
        return companyJoinRequestService.findLatestRequest(state.registrationId(), state.providerUserId());
    }

    @Transactional
    public void submitRegistrationRequest(HttpSession session, String companyName, String companyDomain) {
        FirstLoginCompanySetupState state = getRequiredState(session);
        String normalizedCompanyDomain = normalizeCompanyDomain(companyDomain, state.emailDomain());
        validateNewCompanyDomain(normalizedCompanyDomain);
        companyRegistrationRequestService.submitRequest(state, companyName, normalizedCompanyDomain);
    }

    @Transactional
    public void submitJoinRequest(HttpSession session, String inviteCode) {
        FirstLoginCompanySetupState state = getRequiredState(session);
        Company company = resolveJoinTargetCompany(inviteCode);
        companyJoinRequestService.submitJoinRequest(
                company,
                state.registrationId(),
                state.providerUserId(),
                state.email(),
                state.resolvedName()
        );
    }

    @Transactional
    public void cancelOwnPendingJoinRequest(HttpSession session) {
        FirstLoginCompanySetupState state = getRequiredState(session);
        companyJoinRequestService.cancelOwnPendingRequest(state.registrationId(), state.providerUserId());
    }

    @Transactional
    public User completeSetup(
            HttpSession session,
            String companyName,
            String companyDomain,
            Long existingCompanyId,
            String inviteCode
    ) {
        FirstLoginCompanySetupState state = getRequiredState(session);
        Company company = resolveCompany(state, companyName, companyDomain, existingCompanyId, inviteCode);
        User user = userOAuthAccountService.resolveUserAfterCompanySetup(
                companyInviteCodeService.ensureInviteCode(company),
                state.toAttributes(),
                oauth2AccountService.loginPolicy()
        );
        if (isNewCompanyRegistration(companyName, existingCompanyId, inviteCode) && company.getCreatorUserId() == null) {
            company.assignCreatorUser(user.getId());
            companyRepository.save(company);
        }
        state.markConsumed();
        clearPendingState(session);
        return user;
    }

    public void clearPendingState(HttpSession session) {
        session.removeAttribute(SESSION_ATTRIBUTE);
    }

    private Company resolveCompany(
            FirstLoginCompanySetupState state,
            String companyName,
            String companyDomain,
            Long existingCompanyId,
            String inviteCode
    ) {
        if (existingCompanyId != null) {
            Company existingCompany = companyRepository.findById(existingCompanyId)
                    .orElseThrow(() -> new IllegalArgumentException("Selected company was not found."));
            validateCompanyForDomain(existingCompany, state.emailDomain());
            return existingCompany;
        }

        String normalizedInviteCode = normalizeInviteCode(inviteCode);
        if (normalizedInviteCode != null) {
            return companyRepository.findByInviteCodeIgnoreCase(normalizedInviteCode)
                    .map(company -> {
                        validateInvitedCompany(company);
                        return company;
                    })
                    .orElseThrow(() -> new IllegalArgumentException("Invite code was not found."));
        }

        Optional<Company> existingCompany = companyRepository.findByCompanyDomainIgnoreCase(state.emailDomain());
        if (existingCompany.isPresent()) {
            Company company = existingCompany.get();
            validateCompanyForDomain(company, state.emailDomain());
            return company;
        }

        String normalizedCompanyDomain = normalizeCompanyDomain(companyDomain, state.emailDomain());
        try {
            validateNewCompanyDomain(normalizedCompanyDomain);
            Company savedCompany = companyRepository.save(Company.builder()
                    .companyName(companyName)
                    .companyDomain(normalizedCompanyDomain)
                    .inviteCode(companyInviteCodeService.generateUniqueInviteCode())
                    .status("ACTIVE")
                    .build());
            return savedCompany;
        } catch (DataIntegrityViolationException ex) {
            log.warn("Company creation hit integrity issue, retrying lookup by requested company domain: companyDomain={}",
                    normalizedCompanyDomain, ex);
            return companyRepository.findByCompanyDomainIgnoreCase(normalizedCompanyDomain)
                    .map(company -> {
                        validateCompanyForRegistrationConflict(company, normalizedCompanyDomain);
                        return company;
                    })
                    .orElseThrow(() -> ex);
        }
    }

    private Company resolveJoinTargetCompany(String inviteCode) {
        String normalizedInviteCode = normalizeInviteCode(inviteCode);
        if (normalizedInviteCode == null) {
            throw new IllegalArgumentException("Invite code is required.");
        }

        return companyRepository.findByInviteCodeIgnoreCase(normalizedInviteCode)
                .map(company -> {
                    validateInvitedCompany(company);
                    return company;
                })
                .orElseThrow(() -> new IllegalArgumentException("Invite code was not found."));
    }

    private void validateCompanyForDomain(Company company, String emailDomain) {
        if (!company.isActive()) {
            throw new IllegalStateException("The matched company is inactive.");
        }
        if (company.getCompanyDomain() == null || !company.getCompanyDomain().equalsIgnoreCase(emailDomain)) {
            throw new IllegalArgumentException("Email domain does not match the company domain.");
        }
    }

    private void validateInvitedCompany(Company company) {
        if (!company.isActive()) {
            throw new IllegalStateException("Invite code target company is inactive.");
        }
    }

    private void validateCompanyForRegistrationConflict(Company company, String companyDomain) {
        if (!company.isActive()) {
            throw new IllegalStateException("The requested company is inactive.");
        }
        if (company.getCompanyDomain() == null || !company.getCompanyDomain().equalsIgnoreCase(companyDomain)) {
            throw new IllegalArgumentException("Requested company domain does not match the resolved company.");
        }
    }

    private String normalizeInviteCode(String inviteCode) {
        if (inviteCode == null) {
            return null;
        }
        String normalized = inviteCode.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeCompanyDomain(String companyDomain, String fallbackDomain) {
        if (companyDomain == null || companyDomain.isBlank()) {
            return fallbackDomain;
        }
        return companyDomain.trim().toLowerCase();
    }

    private void validateNewCompanyDomain(String companyDomain) {
        if (companyDomain == null || companyDomain.isBlank()) {
            throw new IllegalArgumentException("Company domain is required.");
        }
        if (!COMPANY_DOMAIN_PATTERN.matcher(companyDomain).matches()) {
            throw new IllegalArgumentException("Company domain format is invalid.");
        }
        if (companyRepository.existsByCompanyDomainIgnoreCase(companyDomain)) {
            throw new IllegalArgumentException("Company domain is already in use.");
        }
    }

    private boolean isNewCompanyRegistration(String companyName, Long existingCompanyId, String inviteCode) {
        return companyName != null && !companyName.isBlank()
                && existingCompanyId == null
                && normalizeInviteCode(inviteCode) == null;
    }
}
