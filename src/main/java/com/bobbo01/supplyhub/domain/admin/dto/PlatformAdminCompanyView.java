package com.bobbo01.supplyhub.domain.admin.dto;

public record PlatformAdminCompanyView(
        Long companyId,
        String companyName,
        String companyDomain,
        String status,
        Long creatorUserId
) {
}
