package com.bobbo01.supplyhub.domain.company.dto;

public record CompanyRegistrationRequestView(
        Long requestId,
        String requesterName,
        String requesterEmail,
        String requestedCompanyName,
        String requestedCompanyDomain,
        String status,
        String reviewMemo,
        String rejectionReason
) {
}
