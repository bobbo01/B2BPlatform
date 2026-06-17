package com.bobbo01.supplyhub.domain.user.service;

import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.role.repository.RoleRepository;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.entity.UserIdentity;
import com.bobbo01.supplyhub.domain.user.repository.UserIdentityRepository;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import com.bobbo01.supplyhub.global.auth.oauth.OAuth2Attributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserOAuthAccountServicePlatformAdminTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserIdentityRepository userIdentityRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserOAuthAccountService userOAuthAccountService;

    @Test
    void createsPlatformAdminForAllowlistedOAuthUser() {
        OAuth2Attributes attributes = new OAuth2Attributes(
                "workspace",
                "provider-user-1",
                "admin@example.com",
                true,
                "Admin",
                null,
                "admin@example.com",
                Map.of(),
                "sub"
        );
        Role role = Role.builder().roleName(RoleNames.PLATFORM_ADMIN).description("platform admin").build();

        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByRoleNameIgnoreCase(RoleNames.PLATFORM_ADMIN)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userIdentityRepository.save(any(UserIdentity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userOAuthAccountService.resolvePlatformAdmin(attributes);

        assertThat(user.isPlatformAdmin()).isTrue();
        assertThat(user.getCompany()).isNull();
        assertThat(user.getEmail()).isEqualTo("admin@example.com");
    }
}
