package com.bobbo01.supplyhub.domain.role.service;

import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleDataInitializerTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ApplicationArguments applicationArguments;

    @Test
    void seedsDocumentRoles() throws Exception {
        when(roleRepository.findByRoleNameIgnoreCase(any())).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var runner = new RoleDataInitializer().roleInitializer(roleRepository);
        runner.run(applicationArguments);

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, times(4)).save(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(Role::getRoleName)
                .containsExactly(
                        RoleNames.PLATFORM_ADMIN,
                        RoleNames.CART_USER,
                        RoleNames.PURCHASER,
                        RoleNames.APPROVER
                );
    }
}
