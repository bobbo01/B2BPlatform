package com.bobbo01.supplyhub.domain.role.service;

import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.role.repository.RoleRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class RoleDataInitializer {

    @Bean
    ApplicationRunner roleInitializer(RoleRepository roleRepository) {
        return args -> defaultRoles().forEach((roleName, description) ->
                roleRepository.findByRoleNameIgnoreCase(roleName)
                        .orElseGet(() -> roleRepository.save(Role.builder()
                                .roleName(roleName)
                                .description(description)
                                .build())));
    }

    private Map<String, String> defaultRoles() {
        Map<String, String> roles = new LinkedHashMap<>();
        roles.put(RoleNames.PLATFORM_ADMIN, "Platform-wide administrator role");
        roles.put(RoleNames.CART_USER, "Company user who can browse products and manage carts");
        roles.put(RoleNames.PURCHASER, "Company user who can place or execute purchases");
        roles.put(RoleNames.APPROVER, "Company user who can approve purchase requests and finalize orders");
        return roles;
    }
}
