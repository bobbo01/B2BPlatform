package com.bobbo01.supplyhub.domain.company.workflow;

import com.bobbo01.supplyhub.global.auth.oauth.OAuth2Attributes;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

public class FirstLoginCompanySetupState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String registrationId;
    private final String providerUserId;
    private final String email;
    private final String emailDomain;
    private final boolean publicEmailDomain;
    private final String resolvedName;
    private final String phone;
    private final boolean emailVerified;
    private final Instant createdAt;
    private final Instant expiresAt;
    private boolean consumed;

    public FirstLoginCompanySetupState(
            String registrationId,
            String providerUserId,
            String email,
            String emailDomain,
            boolean publicEmailDomain,
            String resolvedName,
            String phone,
            boolean emailVerified,
            Instant createdAt,
            Instant expiresAt,
            boolean consumed
    ) {
        this.registrationId = registrationId;
        this.providerUserId = providerUserId;
        this.email = email;
        this.emailDomain = emailDomain;
        this.publicEmailDomain = publicEmailDomain;
        this.resolvedName = resolvedName;
        this.phone = phone;
        this.emailVerified = emailVerified;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.consumed = consumed;
    }

    public OAuth2Attributes toAttributes() {
        return new OAuth2Attributes(
                registrationId,
                providerUserId,
                email,
                emailVerified,
                resolvedName,
                phone,
                email,
                java.util.Map.of(),
                "sub"
        );
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public void markConsumed() {
        this.consumed = true;
    }

    public String registrationId() {
        return registrationId;
    }

    public String providerUserId() {
        return providerUserId;
    }

    public String email() {
        return email;
    }

    public String emailDomain() {
        return emailDomain;
    }

    public boolean publicEmailDomain() {
        return publicEmailDomain;
    }

    public String resolvedName() {
        return resolvedName;
    }

    public String phone() {
        return phone;
    }

    public boolean emailVerified() {
        return emailVerified;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public boolean consumed() {
        return consumed;
    }
}
