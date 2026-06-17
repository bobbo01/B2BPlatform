package com.bobbo01.supplyhub.domain.company.dto;

public record CompanyJoinRequestView(
        Long requestId,
        String companyName,
        String companyDomain,
        String requesterName,
        String requesterEmail,
        String status,
        String requestedRoleName,
        String requestedRoleLabel,
        String reviewMemo,
        String rejectionReason
) {
}
