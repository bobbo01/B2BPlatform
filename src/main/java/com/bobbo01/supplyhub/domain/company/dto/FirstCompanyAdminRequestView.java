package com.bobbo01.supplyhub.domain.company.dto;

public record FirstCompanyAdminRequestView(
        Long requestId,
        Long companyId,
        String companyName,
        String companyDomain,
        Long requesterUserId,
        String requesterName,
        String requesterEmail,
        String status
) {
}
