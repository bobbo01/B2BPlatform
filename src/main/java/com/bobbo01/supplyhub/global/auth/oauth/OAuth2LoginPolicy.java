package com.bobbo01.supplyhub.global.auth.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
// 정책 분리 지점.
// 회사 도메인 검증, 자동 생성, 자동 연결 여부를 코드에 하드코딩하지 않고
// 설정값으로 분리해 Provider가 늘어나도 같은 정책 계층을 재사용한다.
public class OAuth2LoginPolicy {

    private static final Set<String> DEFAULT_PUBLIC_EMAIL_DOMAINS = Set.of(
            "gmail.com",
            "googlemail.com",
            "naver.com",
            "daum.net",
            "hanmail.net",
            "kakao.com",
            "hotmail.com",
            "outlook.com",
            "yahoo.com"
    );

    private final Long defaultCompanyId;
    private final Long defaultRoleId;
    private final String defaultRoleName;
    private final boolean autoProvisioningEnabled;
    private final boolean autoLinkByEmail;
    private final boolean requireCompanyDomainMatch;
    private final boolean allowPublicEmailDomains;
    private final Set<String> allowedDomains;
    private final Set<String> publicEmailDomains;
    private final Set<String> platformAdminEmails;

    public OAuth2LoginPolicy(
            @Value("${app.security.oauth2.default-company-id:1}") Long defaultCompanyId,
            @Value("${app.security.oauth2.default-role-id:1}") Long defaultRoleId,
            @Value("${app.security.oauth2.default-role-name:CART_USER}") String defaultRoleName,
            @Value("${app.security.oauth2.auto-provisioning-enabled:true}") boolean autoProvisioningEnabled,
            @Value("${app.security.oauth2.auto-link-by-email:false}") boolean autoLinkByEmail,
            @Value("${app.security.oauth2.require-company-domain-match:true}") boolean requireCompanyDomainMatch,
            @Value("${app.security.oauth2.allow-public-email-domains:false}") boolean allowPublicEmailDomains,
            @Value("${app.security.oauth2.allowed-domains:}") String allowedDomains,
            @Value("${app.security.oauth2.public-email-domains:gmail.com,googlemail.com,naver.com,daum.net,hanmail.net,kakao.com,hotmail.com,outlook.com,yahoo.com}") String publicEmailDomains,
            @Value("${app.security.oauth2.platform-admin-emails:}") String platformAdminEmails
    ) {
        this.defaultCompanyId = defaultCompanyId;
        this.defaultRoleId = defaultRoleId;
        this.defaultRoleName = defaultRoleName;
        this.autoProvisioningEnabled = autoProvisioningEnabled;
        this.autoLinkByEmail = autoLinkByEmail;
        this.requireCompanyDomainMatch = requireCompanyDomainMatch;
        this.allowPublicEmailDomains = allowPublicEmailDomains;
        this.allowedDomains = Arrays.stream(allowedDomains.split(","))
                .map(String::trim)
                .filter(domain -> !domain.isEmpty())
                .map(domain -> domain.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        this.publicEmailDomains = Arrays.stream(publicEmailDomains.split(","))
                .map(String::trim)
                .filter(domain -> !domain.isEmpty())
                .map(domain -> domain.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        this.platformAdminEmails = Arrays.stream(platformAdminEmails.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .map(email -> email.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public Long defaultCompanyId() {
        return defaultCompanyId;
    }

    public Long defaultRoleId() {
        return defaultRoleId;
    }

    public String defaultRoleName() {
        return defaultRoleName;
    }

    public boolean autoProvisioningEnabled() {
        return autoProvisioningEnabled;
    }

    public boolean autoLinkByEmail() {
        return autoLinkByEmail;
    }

    public boolean requireCompanyDomainMatch() {
        return requireCompanyDomainMatch;
    }

    public boolean isEmailDomainAllowed(String email) {
        String domain = extractDomain(email);
        if (domain == null) {
            return false;
        }
        if (!allowPublicEmailDomains && isPublicEmailDomain(domain)) {
            return false;
        }
        if (allowedDomains.isEmpty()) {
            return true;
        }
        return allowedDomains.contains(domain);
    }

    public boolean allowPublicEmailDomains() {
        return allowPublicEmailDomains;
    }

    public boolean isPlatformAdminEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return platformAdminEmails.contains(email.trim().toLowerCase(Locale.ROOT));
    }

    public boolean isPublicEmailDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            return false;
        }
        return publicEmailDomains.isEmpty()
                ? DEFAULT_PUBLIC_EMAIL_DOMAINS.contains(domain.toLowerCase(Locale.ROOT))
                : publicEmailDomains.contains(domain.toLowerCase(Locale.ROOT));
    }

    public String extractDomain(String email) {
        if (email == null) {
            return null;
        }
        int atIndex = email.lastIndexOf('@');
        if (atIndex < 0 || atIndex == email.length() - 1) {
            return null;
        }
        return email.substring(atIndex + 1).toLowerCase(Locale.ROOT);
    }
}

