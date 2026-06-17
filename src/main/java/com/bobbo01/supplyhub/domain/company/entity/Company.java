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

@Getter
@Entity
@Table(name = "companies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id")
    private Long id;

    @Column(name = "company_name", nullable = false, length = 120)
    private String companyName;

    @Column(name = "company_domain", unique = true, length = 120)
    private String companyDomain;

    @Column(name = "invite_code", unique = true, length = 64)
    private String inviteCode;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "creator_user_id")
    private Long creatorUserId;

    @Builder
    public Company(String companyName, String companyDomain, String inviteCode, String status, Long creatorUserId) {
        this.companyName = companyName;
        this.companyDomain = companyDomain;
        this.inviteCode = inviteCode;
        this.status = status;
        this.creatorUserId = creatorUserId;
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public void activate() {
        this.status = "ACTIVE";
    }

    public void inactivate() {
        this.status = "INACTIVE";
    }

    public void updateInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public void revokeInviteCode() {
        this.inviteCode = null;
    }

    public void updateCompanyProfile(String companyName, String companyDomain) {
        this.companyName = companyName;
        this.companyDomain = companyDomain;
    }

    public void assignCreatorUser(Long creatorUserId) {
        this.creatorUserId = creatorUserId;
    }
}

