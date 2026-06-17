package com.bobbo01.supplyhub.domain.admin.service;

import com.bobbo01.supplyhub.domain.admin.dto.PlatformAdminPageView;
import com.bobbo01.supplyhub.domain.company.service.CompanyRegistrationRequestService;
import com.bobbo01.supplyhub.domain.company.service.FirstCompanyAdminRequestService;
import com.bobbo01.supplyhub.domain.home.dto.WorkspaceUserView;
import com.bobbo01.supplyhub.domain.home.service.HomeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlatformAdminPageService {

    public static final String COMPANIES_SECTION = "companies";
    public static final String USERS_SECTION = "users";
    public static final String ORDERS_SECTION = "orders";
    public static final String SETTLEMENTS_SECTION = "settlements";

    private final HomeService homeService;
    private final PlatformAdminService platformAdminService;
    private final CompanyRegistrationRequestService companyRegistrationRequestService;
    private final FirstCompanyAdminRequestService firstCompanyAdminRequestService;

    public PlatformAdminPageService(
            HomeService homeService,
            PlatformAdminService platformAdminService,
            CompanyRegistrationRequestService companyRegistrationRequestService,
            FirstCompanyAdminRequestService firstCompanyAdminRequestService
    ) {
        this.homeService = homeService;
        this.platformAdminService = platformAdminService;
        this.companyRegistrationRequestService = companyRegistrationRequestService;
        this.firstCompanyAdminRequestService = firstCompanyAdminRequestService;
    }

    @Transactional(readOnly = true)
    public PlatformAdminPageView getCompaniesPage(Long userId) {
        WorkspaceUserView workspaceUser = getRequiredPlatformAdmin(userId);
        return new PlatformAdminPageView(
                workspaceUser,
                COMPANIES_SECTION,
                PlatformAdminService.ORDER_FILTER_PENDING,
                companyRegistrationRequestService.getPendingRequestsForPlatformAdmin(userId),
                firstCompanyAdminRequestService.getPendingRequestsForPlatformAdmin(userId),
                platformAdminService.getCompanyViews(userId),
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of()
        );
    }

    @Transactional(readOnly = true)
    public PlatformAdminPageView getUsersPage(Long userId) {
        WorkspaceUserView workspaceUser = getRequiredPlatformAdmin(userId);
        return new PlatformAdminPageView(
                workspaceUser,
                USERS_SECTION,
                PlatformAdminService.ORDER_FILTER_PENDING,
                List.of(),
                List.of(),
                List.of(),
                platformAdminService.getUserViews(userId),
                platformAdminService.getPurchasingRoleOptions(),
                List.of(),
                null,
                List.of()
        );
    }

    @Transactional(readOnly = true)
    public PlatformAdminPageView getOrdersPage(Long userId, String orderFilter) {
        WorkspaceUserView workspaceUser = getRequiredPlatformAdmin(userId);
        String normalizedOrderFilter = platformAdminService.normalizeOrderFilter(orderFilter);
        return new PlatformAdminPageView(
                workspaceUser,
                ORDERS_SECTION,
                normalizedOrderFilter,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                platformAdminService.getPlatformApprovalOrders(userId, normalizedOrderFilter),
                null,
                List.of()
        );
    }

    @Transactional(readOnly = true)
    public PlatformAdminPageView getSettlementsPage(Long userId) {
        WorkspaceUserView workspaceUser = getRequiredPlatformAdmin(userId);
        return new PlatformAdminPageView(
                workspaceUser,
                SETTLEMENTS_SECTION,
                PlatformAdminService.ORDER_FILTER_PENDING,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                platformAdminService.getSettlementSummary(userId),
                platformAdminService.getSettlementOrders(userId)
        );
    }

    @Transactional(readOnly = true)
    public boolean isPlatformAdmin(Long userId) {
        return homeService.getWorkspaceUser(userId).platformAdmin();
    }

    private WorkspaceUserView getRequiredPlatformAdmin(Long userId) {
        WorkspaceUserView workspaceUser = homeService.getWorkspaceUser(userId);
        if (!workspaceUser.platformAdmin()) {
            throw new IllegalStateException("Only PLATFORM_ADMIN can access admin pages.");
        }
        return workspaceUser;
    }
}
