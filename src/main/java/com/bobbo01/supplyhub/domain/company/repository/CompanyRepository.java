package com.bobbo01.supplyhub.domain.company.repository;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    @Query("""
            select c
            from Company c
            order by lower(c.companyName) asc, c.companyName asc
            """)
    List<Company> findAllOrderByCompanyNameAsc();

    Optional<Company> findByCompanyDomainIgnoreCase(String companyDomain);

    Optional<Company> findByInviteCodeIgnoreCase(String inviteCode);

    boolean existsByCompanyDomainIgnoreCase(String companyDomain);

    boolean existsByCompanyDomainIgnoreCaseAndIdNot(String companyDomain, Long id);

    boolean existsByInviteCodeIgnoreCase(String inviteCode);
}

