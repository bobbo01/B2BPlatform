package com.bobbo01.supplyhub.domain.admin.dto;

import com.bobbo01.supplyhub.domain.company.dto.CompanyRegistrationRequestView;
import com.bobbo01.supplyhub.domain.company.dto.FirstCompanyAdminRequestView;
import com.bobbo01.supplyhub.domain.home.dto.WorkspaceUserView;

import java.util.List;

public record PlatformAdminPageView(
        WorkspaceUserView workspaceUser,
        String activeSection,
        String activeOrderFilter,
        List<CompanyRegistrationRequestView> pendingCompanyRegistrationRequests,
        List<FirstCompanyAdminRequestView> pendingFirstCompanyAdminRequests,
        List<PlatformAdminCompanyView> platformAdminCompanies,
        List<PlatformAdminUserView> platformAdminUsers,
        List<String> purchasingRoleOptions,
        List<PlatformApprovalOrderView> platformApprovalOrders,
        PlatformSettlementSummaryView settlementSummary,
        List<PlatformSettlementOrderView> platformSettlementOrders
) {
}
