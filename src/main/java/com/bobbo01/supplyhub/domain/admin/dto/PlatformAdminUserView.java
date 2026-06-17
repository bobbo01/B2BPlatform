package com.bobbo01.supplyhub.domain.admin.dto;

public record PlatformAdminUserView(
        Long userId,
        String displayName,
        String email,
        String accountType,
        String accountTypeLabel,
        String companyName,
        String status,
        String purchasingRole,
        String purchasingRoleLabel,
        boolean companyAdmin
) {
}
