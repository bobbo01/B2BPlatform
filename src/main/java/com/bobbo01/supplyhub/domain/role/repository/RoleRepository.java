package com.bobbo01.supplyhub.domain.role.repository;

import com.bobbo01.supplyhub.domain.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleNameIgnoreCase(String roleName);
}

