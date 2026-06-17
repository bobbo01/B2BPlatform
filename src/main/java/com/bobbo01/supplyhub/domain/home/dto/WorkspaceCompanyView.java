package com.bobbo01.supplyhub.domain.home.dto;

public record WorkspaceCompanyView(
        Long companyId,
        String companyName,
        String companyDomain,
        String inviteCode,
        String status
) {
}
