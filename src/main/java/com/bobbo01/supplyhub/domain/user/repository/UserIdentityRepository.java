package com.bobbo01.supplyhub.domain.user.repository;

import com.bobbo01.supplyhub.domain.user.entity.UserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {

    Optional<UserIdentity> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<UserIdentity> findAllByUserId(Long userId);
}

