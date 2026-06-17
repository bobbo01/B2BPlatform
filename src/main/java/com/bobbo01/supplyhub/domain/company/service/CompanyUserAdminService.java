package com.bobbo01.supplyhub.domain.company.service;

import com.bobbo01.supplyhub.domain.company.dto.CompanyUserAdminView;
import com.bobbo01.supplyhub.domain.role.RoleLabeler;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.role.repository.RoleRepository;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class CompanyUserAdminService {

    private static final Set<String> PURCHASING_ROLE_NAMES = Set.of(
            RoleNames.CART_USER,
            RoleNames.PURCHASER,
            RoleNames.APPROVER
    );

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public CompanyUserAdminService(
            UserRepository userRepository,
            RoleRepository roleRepository
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<CompanyUserAdminView> getCompanyUserViews(Long reviewerUserId) {
        User reviewer = getRequiredCompanyAdmin(reviewerUserId);
        Long companyId = getRequiredCompanyId(reviewer);
        return userRepository.findAllByCompanyIdOrderByEmailAsc(companyId).stream()
                .map(user -> new CompanyUserAdminView(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getStatus(),
                        user.getPurchasingRole().getRoleName(),
                        RoleLabeler.toRoleLabel(user.getPurchasingRole().getRoleName()),
                        user.hasCompanyAdminRole(),
                        isSelfManagedCompanyAdminLocked(reviewer, user)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getPurchasingRoleOptions() {
        return List.of(RoleNames.CART_USER, RoleNames.PURCHASER, RoleNames.APPROVER);
    }

    @Transactional
    public void updateUserPurchasingRole(Long reviewerUserId, Long targetUserId, String roleName) {
        User reviewer = getRequiredCompanyAdmin(reviewerUserId);
        User target = getRequiredManagedUser(targetUserId, reviewer);
        Role role = roleRepository.findByRoleNameIgnoreCase(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role was not found: " + roleName));
        assertPurchasingRole(role);
        target.changePurchasingRole(role);
    }

    @Transactional
    public void updateUserStatus(Long reviewerUserId, Long targetUserId, String status) {
        User reviewer = getRequiredCompanyAdmin(reviewerUserId);
        User target = getRequiredManagedUser(targetUserId, reviewer);
        if ("ACTIVE".equalsIgnoreCase(status)) {
            target.activate();
            return;
        }
        if ("INACTIVE".equalsIgnoreCase(status)) {
            assertNotSelfManagedCompanyAdmin(reviewer, target);
            assertNotLastActiveCompanyAdmin(target);
            target.inactivate();
            return;
        }
        throw new IllegalArgumentException("Unsupported user status: " + status);
    }

    @Transactional
    public void updateUserCompanyAdmin(Long reviewerUserId, Long targetUserId, boolean companyAdmin) {
        User reviewer = getRequiredCompanyAdmin(reviewerUserId);
        User target = getRequiredManagedUser(targetUserId, reviewer);
        if (companyAdmin) {
            target.grantCompanyAdmin();
            return;
        }
        assertNotSelfManagedCompanyAdmin(reviewer, target);
        assertNotRemovingLastActiveCompanyAdmin(target);
        target.revokeCompanyAdmin();
    }

    private User getRequiredCompanyAdmin(Long userId) {
        User reviewer = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User was not found."));
        if (!reviewer.hasCompanyAdminRole()) {
            throw new IllegalStateException("Only COMPANY_ADMIN can manage company users.");
        }
        if (reviewer.getCompany() == null) {
            throw new IllegalStateException("Company admin must belong to a company.");
        }
        return reviewer;
    }

    private User getRequiredManagedUser(Long targetUserId, User reviewer) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalStateException("User was not found."));
        if (target.isPlatformAdmin() || target.getCompany() == null) {
            throw new IllegalStateException("Only company users in the same company can be managed.");
        }
        if (!getRequiredCompanyId(reviewer).equals(target.getCompany().getId())) {
            throw new IllegalStateException("A company admin can only manage users in their own company.");
        }
        return target;
    }

    private Long getRequiredCompanyId(User user) {
        if (user.getCompany() == null || user.getCompany().getId() == null) {
            throw new IllegalStateException("Company admin must belong to a persisted company.");
        }
        return user.getCompany().getId();
    }

    private void assertNotLastActiveCompanyAdmin(User target) {
        if (!target.hasCompanyAdminRole() || !target.isActive()) {
            return;
        }
        assertNotRemovingLastActiveCompanyAdmin(target);
    }

    private void assertNotRemovingLastActiveCompanyAdmin(User target) {
        if (!target.hasCompanyAdminRole() || !target.isActive()) {
            return;
        }
        long activeCompanyAdminCount = userRepository.countByCompanyIdAndCompanyAdminTrueAndStatusIgnoreCase(
                getRequiredCompanyId(target),
                "ACTIVE"
        );
        if (activeCompanyAdminCount <= 1) {
            throw new IllegalStateException("The last active COMPANY_ADMIN in a company cannot be removed or inactivated.");
        }
    }

    private void assertNotSelfManagedCompanyAdmin(User reviewer, User target) {
        if (isSelfManagedCompanyAdminLocked(reviewer, target)) {
            throw new IllegalStateException("A COMPANY_ADMIN cannot revoke or inactivate their own COMPANY_ADMIN account.");
        }
    }

    private boolean isSelfManagedCompanyAdminLocked(User reviewer, User target) {
        return reviewer.getId() != null
                && reviewer.getId().equals(target.getId())
                && reviewer.hasCompanyAdminRole();
    }

    private void assertPurchasingRole(Role role) {
        if (!PURCHASING_ROLE_NAMES.contains(role.getRoleName())) {
            throw new IllegalArgumentException("Only purchasing roles can be assigned in this operation.");
        }
    }
}
