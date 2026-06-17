package com.bobbo01.supplyhub.domain.user.service;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.company.entity.CompanyJoinRequest;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.role.repository.RoleRepository;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.entity.UserIdentity;
import com.bobbo01.supplyhub.domain.user.repository.UserIdentityRepository;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import com.bobbo01.supplyhub.global.auth.oauth.OAuth2Attributes;
import com.bobbo01.supplyhub.global.auth.oauth.OAuth2LoginPolicy;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserOAuthAccountService {

    private static final Logger log = LoggerFactory.getLogger(UserOAuthAccountService.class);
    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final RoleRepository roleRepository;

    public UserOAuthAccountService(
            UserRepository userRepository,
            UserIdentityRepository userIdentityRepository,
            RoleRepository roleRepository
    ) {
        this.userRepository = userRepository;
        this.userIdentityRepository = userIdentityRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public User resolveUser(Company company, OAuth2Attributes attributes, OAuth2LoginPolicy loginPolicy) {
        UserIdentity identity = userIdentityRepository.findByProviderAndProviderUserId(
                        attributes.registrationId(),
                        attributes.providerUserId()
                )
                .orElse(null);

        if (identity != null) {
            User user = identity.getUser();
            if (isDetachedCompanyUser(user)) {
                attachDetachedUserToCompany(user, company, loginPolicy.defaultRoleName());
                user.updateProfile(attributes.resolvedName(), attributes.phone());
                user.recordLogin();
                identity.updateIdentity(attributes.email(), attributes.emailVerified());
                return user;
            }
            assertCompanyAccess(company, user, loginPolicy);
            assertActiveUser(user);
            user.updateProfile(attributes.resolvedName(), attributes.phone());
            user.recordLogin();
            identity.updateIdentity(attributes.email(), attributes.emailVerified());
            return user;
        }

        User user = linkExistingUser(company, attributes, loginPolicy);
        if (user == null) {
            user = createNewUser(company, attributes, loginPolicy, true);
        }

        UserIdentity newIdentity = UserIdentity.create(
                user,
                attributes.registrationId(),
                attributes.providerUserId(),
                attributes.email(),
                attributes.emailVerified()
        );
        userIdentityRepository.save(newIdentity);

        return user;
    }

    @Transactional
    public User resolvePlatformAdmin(OAuth2Attributes attributes) {
        User existingUser = userRepository.findByEmailIgnoreCase(attributes.email())
                .orElse(null);

        if (existingUser != null) {
            if (!existingUser.isPlatformAdmin()) {
                throw authException("platform_admin_conflict", "Email is already used by a company user.");
            }
            existingUser.updateProfile(attributes.resolvedName(), attributes.phone());
            existingUser.recordLogin();

            userIdentityRepository.findByProviderAndProviderUserId(attributes.registrationId(), attributes.providerUserId())
                    .ifPresentOrElse(
                            identity -> identity.updateIdentity(attributes.email(), attributes.emailVerified()),
                            () -> userIdentityRepository.save(UserIdentity.create(
                                    existingUser,
                                    attributes.registrationId(),
                                    attributes.providerUserId(),
                                    attributes.email(),
                                    attributes.emailVerified()
                            ))
                    );
            return existingUser;
        }

        Role platformAdminRole = roleRepository.findByRoleNameIgnoreCase(RoleNames.PLATFORM_ADMIN)
                .orElseThrow(() -> authException("role_not_found", "PLATFORM_ADMIN role not found."));

        User platformAdmin = userRepository.save(User.createPlatformAdmin(
                platformAdminRole,
                attributes.email(),
                attributes.resolvedName(),
                attributes.phone()
        ));
        userIdentityRepository.save(UserIdentity.create(
                platformAdmin,
                attributes.registrationId(),
                attributes.providerUserId(),
                attributes.email(),
                attributes.emailVerified()
        ));
        return platformAdmin;
    }

    @Transactional
    public Optional<User> resolveExistingUserByIdentity(OAuth2Attributes attributes) {
        return userIdentityRepository.findByProviderAndProviderUserId(
                        attributes.registrationId(),
                        attributes.providerUserId()
                )
                .flatMap(identity -> {
                    User user = identity.getUser();
                    assertActiveUser(user);
                    if (isDetachedCompanyUser(user)) {
                        return Optional.empty();
                    }
                    user.updateProfile(attributes.resolvedName(), attributes.phone());
                    user.recordLogin();
                    identity.updateIdentity(attributes.email(), attributes.emailVerified());
                    return Optional.of(user);
                });
    }

    @Transactional
    public User resolveUserAfterCompanySetup(Company company, OAuth2Attributes attributes, OAuth2LoginPolicy loginPolicy) {
        UserIdentity identity = userIdentityRepository.findByProviderAndProviderUserId(
                        attributes.registrationId(),
                        attributes.providerUserId()
                )
                .orElse(null);

        if (identity != null) {
            User user = identity.getUser();
            if (isDetachedCompanyUser(user)) {
                attachDetachedUserToCompany(user, company, loginPolicy.defaultRoleName());
                user.updateProfile(attributes.resolvedName(), attributes.phone());
                user.recordLogin();
                identity.updateIdentity(attributes.email(), attributes.emailVerified());
                return user;
            }
            assertExactCompanyAccess(company, user);
            assertActiveUser(user);
            user.updateProfile(attributes.resolvedName(), attributes.phone());
            user.recordLogin();
            identity.updateIdentity(attributes.email(), attributes.emailVerified());
            return user;
        }

        User user = userRepository.findByCompanyIdAndEmailIgnoreCase(company.getId(), attributes.email())
                .map(existingUser -> {
                    assertActiveUser(existingUser);
                    existingUser.updateProfile(attributes.resolvedName(), attributes.phone());
                    existingUser.recordLogin();
                    return existingUser;
                })
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(attributes.email())
                        .map(existingUser -> {
                            if (!isDetachedCompanyUser(existingUser)) {
                                throw authException("account_conflict", "Email is already used by another linked account.");
                            }
                            attachDetachedUserToCompany(existingUser, company, loginPolicy.defaultRoleName());
                            existingUser.updateProfile(attributes.resolvedName(), attributes.phone());
                            existingUser.recordLogin();
                            return existingUser;
                        })
                        .orElseGet(() -> createNewUser(company, attributes, loginPolicy, false)));

        UserIdentity newIdentity = UserIdentity.create(
                user,
                attributes.registrationId(),
                attributes.providerUserId(),
                attributes.email(),
                attributes.emailVerified()
        );
        userIdentityRepository.save(newIdentity);

        return user;
    }

    @Transactional
    public User provisionApprovedCompanyJoinRequest(CompanyJoinRequest joinRequest) {
        UserIdentity identity = userIdentityRepository.findByProviderAndProviderUserId(
                        joinRequest.getProvider(),
                        joinRequest.getProviderUserId()
                )
                .orElse(null);

        if (identity != null) {
            User linkedUser = identity.getUser();
            if (isDetachedCompanyUser(linkedUser)) {
                attachDetachedUserToCompany(linkedUser, joinRequest.getCompany(), joinRequest.getRequestedRoleName());
                linkedUser.updateProfile(joinRequest.getRequestedName(), linkedUser.getPhone());
                identity.updateIdentity(joinRequest.getRequestedEmail(), true);
                return linkedUser;
            }
            assertExactCompanyAccess(joinRequest.getCompany(), linkedUser);
            assertActiveUser(linkedUser);
            linkedUser.updateProfile(joinRequest.getRequestedName(), linkedUser.getPhone());
            identity.updateIdentity(joinRequest.getRequestedEmail(), true);
            return linkedUser;
        }

        User user = userRepository.findByEmailIgnoreCase(joinRequest.getRequestedEmail())
                .map(existingUser -> {
                    if (isDetachedCompanyUser(existingUser)) {
                        attachDetachedUserToCompany(existingUser, joinRequest.getCompany(), joinRequest.getRequestedRoleName());
                        existingUser.updateProfile(joinRequest.getRequestedName(), existingUser.getPhone());
                        return existingUser;
                    }
                    assertExactCompanyAccess(joinRequest.getCompany(), existingUser);
                    assertActiveUser(existingUser);
                    existingUser.updateProfile(joinRequest.getRequestedName(), existingUser.getPhone());
                    return existingUser;
                })
                .orElseGet(() -> createCompanyUser(
                        joinRequest.getCompany(),
                        joinRequest.getRequestedRoleName(),
                        joinRequest.getRequestedEmail(),
                        joinRequest.getRequestedName(),
                        null
                ));

        userIdentityRepository.save(UserIdentity.create(
                user,
                joinRequest.getProvider(),
                joinRequest.getProviderUserId(),
                joinRequest.getRequestedEmail(),
                true
        ));
        return user;
    }

    private User linkExistingUser(Company company, OAuth2Attributes attributes, OAuth2LoginPolicy loginPolicy) {
        return userRepository.findByCompanyIdAndEmailIgnoreCase(company.getId(), attributes.email())
                .map(existingUser -> {
                    if (!loginPolicy.autoLinkByEmail()) {
                        log.warn("Existing user found but auto-link by email is disabled: companyId={}, userId={}, email={}",
                                company.getId(), existingUser.getId(), attributes.email());
                        throw authException(
                                "account_link_required",
                                "Existing user found. Automatic email linking is disabled."
                        );
                    }
                    assertActiveUser(existingUser);
                    existingUser.updateProfile(attributes.resolvedName(), attributes.phone());
                    existingUser.recordLogin();
                    return existingUser;
                })
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(attributes.email())
                        .map(existingUser -> {
                            if (!isDetachedCompanyUser(existingUser)) {
                                throw authException("account_conflict", "Email is already used by another linked account.");
                            }
                            if (!loginPolicy.autoLinkByEmail()) {
                                log.warn("Detached user found but auto-link by email is disabled: companyId={}, userId={}, email={}",
                                        company.getId(), existingUser.getId(), attributes.email());
                                throw authException(
                                        "account_link_required",
                                        "Existing detached user found. Automatic email linking is disabled."
                                );
                            }
                            attachDetachedUserToCompany(existingUser, company, loginPolicy.defaultRoleName());
                            existingUser.updateProfile(attributes.resolvedName(), attributes.phone());
                            existingUser.recordLogin();
                            return existingUser;
                        })
                        .orElse(null));
    }

    private User createNewUser(Company company, OAuth2Attributes attributes, OAuth2LoginPolicy loginPolicy, boolean requireAutoProvisioningEnabled) {
        if (requireAutoProvisioningEnabled && !loginPolicy.autoProvisioningEnabled()) {
            throw authException("provisioning_disabled", "Automatic provisioning is disabled for this application.");
        }

        return createCompanyUser(
                company,
                loginPolicy.defaultRoleName(),
                attributes.email(),
                attributes.resolvedName(),
                attributes.phone()
        );
    }

    private User createCompanyUser(Company company, String roleName, String email, String fullName, String phone) {
        Role role = resolveRole(roleName);

        User savedUser = userRepository.save(User.createOAuthUser(
                company,
                role,
                email,
                fullName,
                phone
        ));
        return savedUser;
    }

    private void attachDetachedUserToCompany(User user, Company company, String roleName) {
        assertActiveUser(user);
        if (!isDetachedCompanyUser(user)) {
            throw authException("account_conflict", "User is already linked to another company.");
        }
        user.attachToCompany(company, resolveRole(roleName));
    }

    private boolean isDetachedCompanyUser(User user) {
        return !user.isPlatformAdmin() && user.getCompany() == null;
    }

    private Role resolveRole(String roleName) {
        return roleRepository.findByRoleNameIgnoreCase(roleName)
                .orElseThrow(() -> authException("role_not_found", "Role not found: " + roleName));
    }

    private void assertCompanyAccess(Company company, User user, OAuth2LoginPolicy loginPolicy) {
        if (!loginPolicy.requireCompanyDomainMatch()) {
            return;
        }
        if (!company.getId().equals(user.getCompany().getId())) {
            throw authException("company_mismatch", "User does not belong to the resolved company.");
        }
    }

    private void assertExactCompanyAccess(Company company, User user) {
        if (!company.getId().equals(user.getCompany().getId())) {
            throw authException("company_mismatch", "User does not belong to the selected company.");
        }
    }

    private void assertActiveUser(User user) {
        if (!user.isActive()) {
            throw authException("user_inactive", "User is not active: " + user.getEmail());
        }
    }

    private OAuth2AuthenticationException authException(String code, String message) {
        return new OAuth2AuthenticationException(new OAuth2Error(code), message);
    }
}
