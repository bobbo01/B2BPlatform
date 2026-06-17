package com.bobbo01.supplyhub.domain.company.service;

import com.bobbo01.supplyhub.domain.company.dto.WorkspacePageView;
import com.bobbo01.supplyhub.domain.commerce.dto.CommerceWorkspaceView;
import com.bobbo01.supplyhub.domain.commerce.service.CommerceWorkflowService;
import com.bobbo01.supplyhub.domain.home.dto.WorkspaceUserView;
import com.bobbo01.supplyhub.domain.home.service.HomeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkspaceViewService {

    private static final String OVERVIEW_SECTION = "overview";
    private static final String COMPANY_USERS_SECTION = "company-users";
    private static final String COMMERCE_SECTION = "commerce";
    private static final String COMMERCE_CART_SECTION = "cart";
    private static final String COMMERCE_PURCHASE_REQUESTS_SECTION = "purchase-requests";
    private static final String COMMERCE_APPROVALS_SECTION = "approvals";
    private static final String COMMERCE_ORDER_DRAFTS_SECTION = "order-drafts";

    private final FirstCompanyAdminRequestService firstCompanyAdminRequestService;
    private final CompanyJoinRequestService companyJoinRequestService;
    private final CompanyUserAdminService companyUserAdminService;
    private final HomeService homeService;
    private final CommerceWorkflowService commerceWorkflowService;

    public WorkspaceViewService(
            FirstCompanyAdminRequestService firstCompanyAdminRequestService,
            CompanyJoinRequestService companyJoinRequestService,
            CompanyUserAdminService companyUserAdminService,
            HomeService homeService,
            CommerceWorkflowService commerceWorkflowService
    ) {
        this.firstCompanyAdminRequestService = firstCompanyAdminRequestService;
        this.companyJoinRequestService = companyJoinRequestService;
        this.companyUserAdminService = companyUserAdminService;
        this.homeService = homeService;
        this.commerceWorkflowService = commerceWorkflowService;
    }

    @Transactional(readOnly = true)
    public WorkspacePageView getWorkspacePage(Long userId, String section, String commerceSection, Long orderId) {
        WorkspaceUserView workspaceUser = homeService.getWorkspaceUser(userId);
        if (workspaceUser.platformAdmin()) {
            return new WorkspacePageView(
                    workspaceUser,
                    OVERVIEW_SECTION,
                    false,
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    null,
                    COMMERCE_CART_SECTION,
                    null
            );
        }

        String activeSection = resolveWorkspaceSection(workspaceUser, section);
        boolean canManageCompanyUsers = workspaceUser.companyAdmin();
        boolean canCreateFirstCompanyAdminRequest = firstCompanyAdminRequestService.canCreateRequest(userId);
        CommerceWorkspaceView commerceView = commerceWorkflowService.getWorkspaceView(userId);
        String activeCommerceSection = resolveCommerceSection(commerceView, commerceSection);
        Long selectedPurchaseOrderId = shouldLoadPurchaseOrderDetail(activeSection, activeCommerceSection, orderId)
                ? orderId
                : null;

        return new WorkspacePageView(
                workspaceUser,
                activeSection,
                canCreateFirstCompanyAdminRequest,
                firstCompanyAdminRequestService.findLatestRequestForUser(userId).orElse(null),
                canManageCompanyUsers ? companyJoinRequestService.getPendingRequestsForCompany(userId) : List.of(),
                canManageCompanyUsers ? companyUserAdminService.getCompanyUserViews(userId) : List.of(),
                canManageCompanyUsers ? companyUserAdminService.getPurchasingRoleOptions() : List.of(),
                commerceView,
                activeCommerceSection,
                selectedPurchaseOrderId
        );
    }

    private String resolveWorkspaceSection(WorkspaceUserView workspaceUser, String section) {
        if (COMPANY_USERS_SECTION.equalsIgnoreCase(section) && workspaceUser.companyAdmin()) {
            return COMPANY_USERS_SECTION;
        }
        if (COMMERCE_SECTION.equalsIgnoreCase(section)) {
            return COMMERCE_SECTION;
        }
        if (OVERVIEW_SECTION.equalsIgnoreCase(section)) {
            return OVERVIEW_SECTION;
        }
        return workspaceUser.companyAdmin() ? OVERVIEW_SECTION : COMMERCE_SECTION;
    }

    private String resolveCommerceSection(CommerceWorkspaceView commerceView, String commerceSection) {
        if (commerceView == null) {
            return COMMERCE_CART_SECTION;
        }
        if (COMMERCE_PURCHASE_REQUESTS_SECTION.equalsIgnoreCase(commerceSection)) {
            return COMMERCE_PURCHASE_REQUESTS_SECTION;
        }
        if (COMMERCE_APPROVALS_SECTION.equalsIgnoreCase(commerceSection)) {
            return commerceView.canApprove() ? COMMERCE_APPROVALS_SECTION : COMMERCE_CART_SECTION;
        }
        if (COMMERCE_ORDER_DRAFTS_SECTION.equalsIgnoreCase(commerceSection)) {
            return COMMERCE_ORDER_DRAFTS_SECTION;
        }
        return COMMERCE_CART_SECTION;
    }

    private boolean shouldLoadPurchaseOrderDetail(String activeSection, String activeCommerceSection, Long orderId) {
        return COMMERCE_SECTION.equalsIgnoreCase(activeSection)
                && COMMERCE_ORDER_DRAFTS_SECTION.equalsIgnoreCase(activeCommerceSection)
                && orderId != null;
    }
}
