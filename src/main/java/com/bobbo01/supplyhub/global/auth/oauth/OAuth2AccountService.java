package com.bobbo01.supplyhub.global.auth.oauth;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.repository.CompanyRepository;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.service.UserOAuthAccountService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OAuth2AccountService {

    private final UserOAuthAccountService userOAuthAccountService;
    private final CompanyRepository companyRepository;
    private final OAuth2LoginPolicy loginPolicy;

    public OAuth2AccountService(
            UserOAuthAccountService userOAuthAccountService,
            CompanyRepository companyRepository,
            OAuth2LoginPolicy loginPolicy
    ) {
        this.userOAuthAccountService = userOAuthAccountService;
        this.companyRepository = companyRepository;
        this.loginPolicy = loginPolicy;
    }

    public Optional<Company> findActiveCompanyByEmailDomain(OAuth2Attributes attributes) {
        validateAllowedDomain(attributes);
        String emailDomain = extractRequiredDomain(attributes);
        if (loginPolicy.isPublicEmailDomain(emailDomain)) {
            return Optional.empty();
        }

        return companyRepository.findByCompanyDomainIgnoreCase(emailDomain)
                .map(company -> {
                    if (!company.isActive()) {
                        throw authException("company_inactive", "Company is not active: " + company.getCompanyName());
                    }
                    return company;
                });
    }

    public Optional<User> resolveExistingUserByIdentity(OAuth2Attributes attributes) {
        return userOAuthAccountService.resolveExistingUserByIdentity(attributes);
    }

    public boolean isPlatformAdminEmail(OAuth2Attributes attributes) {
        return loginPolicy.isPlatformAdminEmail(attributes.email());
    }

    public User resolvePlatformAdmin(OAuth2Attributes attributes) {
        return userOAuthAccountService.resolvePlatformAdmin(attributes);
    }

    public User resolveUser(Company company, OAuth2Attributes attributes) {
        return userOAuthAccountService.resolveUser(company, attributes, loginPolicy);
    }

    public String extractRequiredDomain(OAuth2Attributes attributes) {
        String emailDomain = loginPolicy.extractDomain(attributes.email());
        if (emailDomain == null) {
            throw authException("invalid_email_domain", "Unable to determine company domain from email.");
        }
        return emailDomain;
    }

    public OAuth2LoginPolicy loginPolicy() {
        return loginPolicy;
    }

    private void validateAllowedDomain(OAuth2Attributes attributes) {
        if (!loginPolicy.isEmailDomainAllowed(attributes.email())) {
            throw authException("domain_not_allowed", "Email domain is not allowed: " + attributes.email());
        }
    }

    private OAuth2AuthenticationException authException(String code, String message) {
        return new OAuth2AuthenticationException(new OAuth2Error(code), message);
    }
}
