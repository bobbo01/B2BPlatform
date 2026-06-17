package com.bobbo01.supplyhub.domain.user.entity;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
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
import java.util.Set;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    private static final Set<String> PURCHASING_ROLE_NAMES = Set.of(
            RoleNames.CART_USER,
            RoleNames.PURCHASER,
            RoleNames.APPROVER
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "company_admin")
    private Boolean companyAdmin;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 50)
    private String phone;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder
    public User(
            Company company,
            Role role,
            Boolean companyAdmin,
            String email,
            String fullName,
            String phone,
            String status,
            LocalDateTime lastLoginAt
    ) {
        validateRoleConstraints(company, role, companyAdmin);
        this.company = company;
        this.role = role;
        this.companyAdmin = companyAdmin;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.status = status;
        this.lastLoginAt = lastLoginAt;
    }

    public static User createOAuthUser(Company company, Role role, String email, String fullName, String phone) {
        return User.builder()
                .company(company)
                .role(role)
                .companyAdmin(false)
                .email(email)
                .fullName(fullName)
                .phone(phone)
                .status("ACTIVE")
                .lastLoginAt(LocalDateTime.now())
                .build();
    }

    public static User createPlatformAdmin(Role role, String email, String fullName, String phone) {
        return User.builder()
                .company(null)
                .role(role)
                .companyAdmin(false)
                .email(email)
                .fullName(fullName)
                .phone(phone)
                .status("ACTIVE")
                .lastLoginAt(LocalDateTime.now())
                .build();
    }

    public void updateProfile(String fullName, String phone) {
        this.fullName = fullName;
        this.phone = phone;
    }

    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = "ACTIVE";
    }

    public void inactivate() {
        this.status = "INACTIVE";
    }

    public void leaveCompany() {
        if (isPlatformAdmin()) {
            throw new IllegalStateException("PLATFORM_ADMIN user cannot leave a company.");
        }
        this.company = null;
        this.companyAdmin = false;
    }

    public void attachToCompany(Company company, Role role) {
        if (company == null) {
            throw new IllegalArgumentException("Company is required.");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }
        if (RoleNames.PLATFORM_ADMIN.equalsIgnoreCase(role.getRoleName())) {
            throw new IllegalArgumentException("Company user cannot be assigned PLATFORM_ADMIN role.");
        }
        this.company = company;
        this.role = role;
        this.companyAdmin = false;
    }

    public void grantCompanyAdmin() {
        if (isPlatformAdmin()) {
            throw new IllegalStateException("PLATFORM_ADMIN user cannot become COMPANY_ADMIN.");
        }
        this.companyAdmin = true;
    }

    public void revokeCompanyAdmin() {
        this.companyAdmin = false;
    }

    public void changePurchasingRole(Role role) {
        if (isPlatformAdmin()) {
            throw new IllegalStateException("PLATFORM_ADMIN user does not have a purchasing role.");
        }
        if (role == null || !PURCHASING_ROLE_NAMES.contains(role.getRoleName())) {
            throw new IllegalArgumentException("Only purchasing roles can be assigned to company users.");
        }
        this.role = role;
    }

    public Role getPurchasingRole() {
        return role;
    }

    public String getPurchasingRoleName() {
        return role != null ? role.getRoleName() : null;
    }

    public boolean hasPurchasingRole(String roleName) {
        return roleName != null && roleName.equalsIgnoreCase(getPurchasingRoleName());
    }

    public boolean hasAnyPurchasingRole(String... roleNames) {
        if (roleNames == null || roleNames.length == 0) {
            return false;
        }
        for (String roleName : roleNames) {
            if (hasPurchasingRole(roleName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCompanyUser() {
        return !isPlatformAdmin() && company != null;
    }

    public boolean hasCompanyAdminRole() {
        return Boolean.TRUE.equals(companyAdmin);
    }

    public boolean isPlatformAdmin() {
        return hasRoleName(RoleNames.PLATFORM_ADMIN);
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    private boolean hasRoleName(String roleName) {
        return role != null && roleName.equalsIgnoreCase(role.getRoleName());
    }

    private void validateRoleConstraints(Company company, Role role, Boolean companyAdmin) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }

        if (RoleNames.PLATFORM_ADMIN.equalsIgnoreCase(role.getRoleName())) {
            if (company != null) {
                throw new IllegalArgumentException("PLATFORM_ADMIN user cannot belong to a company.");
            }
            if (Boolean.TRUE.equals(companyAdmin)) {
                throw new IllegalArgumentException("PLATFORM_ADMIN user cannot also be COMPANY_ADMIN.");
            }
            return;
        }

        if (RoleNames.COMPANY_ADMIN.equalsIgnoreCase(role.getRoleName())) {
            throw new IllegalArgumentException("COMPANY_ADMIN is an operational flag, not a persisted purchasing role.");
        }

        if (company == null && Boolean.TRUE.equals(companyAdmin)) {
            throw new IllegalArgumentException("Company-less user cannot be COMPANY_ADMIN.");
        }
    }
}

