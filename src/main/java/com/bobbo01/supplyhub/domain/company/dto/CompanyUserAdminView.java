package com.bobbo01.supplyhub.domain.company.dto;

public record CompanyUserAdminView(
        Long userId,
        String fullName,
        String email,
        String status,
        String purchasingRoleName,
        String purchasingRoleLabel,
        boolean companyAdmin,
        boolean selfManagedCompanyAdminLocked
) {
}
