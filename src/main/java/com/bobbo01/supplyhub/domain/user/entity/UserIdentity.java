package com.bobbo01.supplyhub.domain.user.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "user_identities",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_identity_provider_subject", columnNames = {"provider", "provider_user_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserIdentity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_identity_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder
    public UserIdentity(User user, String provider, String providerUserId, String email, Boolean emailVerified,
                        LocalDateTime lastLoginAt) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.emailVerified = emailVerified;
        this.lastLoginAt = lastLoginAt;
    }

    public static UserIdentity create(User user, String provider, String providerUserId, String email, boolean emailVerified) {
        return UserIdentity.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .email(email)
                .emailVerified(emailVerified)
                .lastLoginAt(LocalDateTime.now())
                .build();
    }

    public void updateIdentity(String email, boolean emailVerified) {
        this.email = email;
        this.emailVerified = emailVerified;
        this.lastLoginAt = LocalDateTime.now();
    }

    public void attachTo(User user) {
        this.user = user;
    }
}

