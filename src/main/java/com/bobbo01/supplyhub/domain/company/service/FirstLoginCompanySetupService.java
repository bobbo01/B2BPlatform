package com.bobbo01.supplyhub.domain.company.service;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.repository.CompanyRepository;
import com.bobbo01.supplyhub.domain.company.workflow.FirstLoginCompanySetupState;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.service.UserOAuthAccountService;
import com.bobbo01.supplyhub.global.auth.oauth.OAuth2AccountService;
import com.bobbo01.supplyhub.global.auth.oauth.OAuth2Attributes;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class FirstLoginCompanySetupService {

    public static final String SESSION_ATTRIBUTE = "firstLoginCompanySetupState";
    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    private final CompanyRepository companyRepository;
    private final UserOAuthAccountService userOAuthAccountService;
    private final OAuth2AccountService oauth2AccountService;

    public FirstLoginCompanySetupService(
            CompanyRepository companyRepository,
            UserOAuthAccountService userOAuthAccountService,
            OAuth2AccountService oauth2AccountService
    ) {
        this.companyRepository = companyRepository;
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
            throw new IllegalStateException("최초 로그인 회사 설정 상태가 없습니다.");
        }
        if (state.consumed() || state.isExpired(Instant.now())) {
            clearPendingState(session);
            throw new IllegalStateException("최초 로그인 회사 설정 상태가 만료되었습니다.");
        }
        return state;
    }

    public Optional<Company> findExistingCompany(HttpSession session) {
        FirstLoginCompanySetupState state = getRequiredState(session);
        return companyRepository.findByCompanyDomainIgnoreCase(state.emailDomain())
                .filter(Company::isActive);
    }

    @Transactional
    public User completeSetup(HttpSession session, String companyName, Long existingCompanyId) {
        FirstLoginCompanySetupState state = getRequiredState(session);
        Company company = resolveCompany(state, companyName, existingCompanyId);
        User user = userOAuthAccountService.resolveUserAfterCompanySetup(
                company,
                state.toAttributes(),
                oauth2AccountService.loginPolicy()
        );
        state.markConsumed();
        clearPendingState(session);
        return user;
    }

    public void clearPendingState(HttpSession session) {
        session.removeAttribute(SESSION_ATTRIBUTE);
    }

    private Company resolveCompany(FirstLoginCompanySetupState state, String companyName, Long existingCompanyId) {
        if (existingCompanyId != null) {
            Company existingCompany = companyRepository.findById(existingCompanyId)
                    .orElseThrow(() -> new IllegalArgumentException("선택한 회사를 찾을 수 없습니다."));
            validateCompanyForDomain(existingCompany, state.emailDomain());
            return existingCompany;
        }

        Optional<Company> existingCompany = companyRepository.findByCompanyDomainIgnoreCase(state.emailDomain());
        if (existingCompany.isPresent()) {
            Company company = existingCompany.get();
            validateCompanyForDomain(company, state.emailDomain());
            return company;
        }

        try {
            return companyRepository.save(Company.builder()
                    .companyName(companyName)
                    .companyDomain(state.emailDomain())
                    .status("ACTIVE")
                    .build());
        } catch (DataIntegrityViolationException ex) {
            return companyRepository.findByCompanyDomainIgnoreCase(state.emailDomain())
                    .map(company -> {
                        validateCompanyForDomain(company, state.emailDomain());
                        return company;
                    })
                    .orElseThrow(() -> ex);
        }
    }

    private void validateCompanyForDomain(Company company, String emailDomain) {
        if (!company.isActive()) {
            throw new IllegalStateException("이미 등록된 회사가 비활성 상태입니다.");
        }
        if (company.getCompanyDomain() == null || !company.getCompanyDomain().equalsIgnoreCase(emailDomain)) {
            throw new IllegalArgumentException("외부 인증 이메일 도메인과 회사 도메인이 일치하지 않습니다.");
        }
    }
}
