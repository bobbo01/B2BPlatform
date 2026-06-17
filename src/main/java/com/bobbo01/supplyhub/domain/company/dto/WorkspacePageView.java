package com.bobbo01.supplyhub.domain.company.dto;

import com.bobbo01.supplyhub.domain.commerce.dto.CommerceWorkspaceView;
import com.bobbo01.supplyhub.domain.home.dto.WorkspaceUserView;

import java.util.List;

public record WorkspacePageView(
        WorkspaceUserView workspaceUser,
        String activeSection,
        boolean canCreateFirstCompanyAdminRequest,
        FirstCompanyAdminRequestView firstCompanyAdminRequest,
        List<CompanyJoinRequestView> pendingCompanyJoinRequests,
        List<CompanyUserAdminView> companyUserAdminUsers,
        List<String> companyUserPurchasingRoleOptions,
        CommerceWorkspaceView commerceView,
        String activeCommerceSection,
        Long selectedPurchaseOrderId
) {
}
