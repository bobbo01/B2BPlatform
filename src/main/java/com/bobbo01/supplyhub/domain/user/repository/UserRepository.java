package com.bobbo01.supplyhub.domain.user.repository;

import com.bobbo01.supplyhub.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "company")
    @Query("select u from User u")
    List<User> findAllWithCompany();

    @EntityGraph(attributePaths = "company")
    @Query("""
            select u
            from User u
            order by lower(u.email) asc, u.email asc
            """)
    List<User> findAllWithCompanyOrderByEmailAsc();

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByCompanyIdAndEmailIgnoreCase(Long companyId, String email);

    long countByCompanyIdAndCompanyAdminTrueAndStatusIgnoreCase(Long companyId, String status);

    List<User> findAllByCompanyIdOrderByEmailAsc(Long companyId);

    List<User> findAllByCompanyIdAndStatusIgnoreCaseAndRoleRoleNameOrderByIdAsc(Long companyId, String status, String roleName);
}

