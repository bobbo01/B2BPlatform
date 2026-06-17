package com.bobbo01.supplyhub.domain.home.dto;

public record WorkspaceUserView(
        Long userId,
        String displayName,
        String email,
        boolean platformAdmin,
        String roleLabel,
        boolean companyAdmin,
        WorkspaceCompanyView company
) {
}
