package com.bobbo01.supplyhub.domain.admin.controller;

import com.bobbo01.supplyhub.domain.admin.dto.PlatformAdminPageView;
import com.bobbo01.supplyhub.domain.admin.service.PlatformAdminPageService;
import com.bobbo01.supplyhub.domain.admin.service.PlatformAdminService;
import com.bobbo01.supplyhub.domain.company.service.CompanyRegistrationRequestService;
import com.bobbo01.supplyhub.domain.company.service.FirstCompanyAdminRequestService;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class PlatformAdminController {

    private final PlatformAdminPageService platformAdminPageService;
    private final PlatformAdminService platformAdminService;
    private final CompanyRegistrationRequestService companyRegistrationRequestService;
    private final FirstCompanyAdminRequestService firstCompanyAdminRequestService;

    public PlatformAdminController(
            PlatformAdminPageService platformAdminPageService,
            PlatformAdminService platformAdminService,
            CompanyRegistrationRequestService companyRegistrationRequestService,
            FirstCompanyAdminRequestService firstCompanyAdminRequestService
    ) {
        this.platformAdminPageService = platformAdminPageService;
        this.platformAdminService = platformAdminService;
        this.companyRegistrationRequestService = companyRegistrationRequestService;
        this.firstCompanyAdminRequestService = firstCompanyAdminRequestService;
    }

    @GetMapping
    public String adminHome() {
        return "redirect:/admin/companies";
    }

    @GetMapping("/companies")
    public String companies(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            Model model
    ) {
        String redirectPath = validatePlatformAdmin(principal);
        if (redirectPath != null) {
            return redirectPath;
        }

        PlatformAdminPageView pageView = platformAdminPageService.getCompaniesPage(principal.getUserId());
        populateAdminPageModel(model, pageView);
        return "pages/admin";
    }

    @GetMapping("/users")
    public String users(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            Model model
    ) {
        String redirectPath = validatePlatformAdmin(principal);
        if (redirectPath != null) {
            return redirectPath;
        }

        populateAdminPageModel(model, platformAdminPageService.getUsersPage(principal.getUserId()));
        return "pages/admin";
    }

    @GetMapping("/orders")
    public String orders(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(name = "orderFilter", required = false) String orderFilter,
            Model model
    ) {
        String redirectPath = validatePlatformAdmin(principal);
        if (redirectPath != null) {
            return redirectPath;
        }

        populateAdminPageModel(model, platformAdminPageService.getOrdersPage(principal.getUserId(), orderFilter));
        return "pages/admin";
    }

    @GetMapping("/settlements")
    public String settlements(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            Model model
    ) {
        String redirectPath = validatePlatformAdmin(principal);
        if (redirectPath != null) {
            return redirectPath;
        }

        populateAdminPageModel(model, platformAdminPageService.getSettlementsPage(principal.getUserId()));
        return "pages/admin";
    }

    @PostMapping("/company-registration-request/approve")
    public String approveCompanyRegistrationRequest(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("requestId") Long requestId,
            @RequestParam(name = "reviewMemo", required = false) String reviewMemo
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        companyRegistrationRequestService.approveRequest(principal.getUserId(), requestId, reviewMemo);
        return "redirect:/admin/companies";
    }

    @PostMapping("/company-registration-request/reject")
    public String rejectCompanyRegistrationRequest(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("requestId") Long requestId,
            @RequestParam(name = "reviewMemo", required = false) String reviewMemo,
            @RequestParam("rejectionReason") String rejectionReason
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        companyRegistrationRequestService.rejectRequest(principal.getUserId(), requestId, reviewMemo, rejectionReason);
        return "redirect:/admin/companies";
    }

    @PostMapping("/first-company-admin-request/approve")
    public String approveFirstCompanyAdminRequest(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("requestId") Long requestId
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        firstCompanyAdminRequestService.approveRequest(principal.getUserId(), requestId);
        return "redirect:/admin/companies";
    }

    @PostMapping("/first-company-admin-request/reject")
    public String rejectFirstCompanyAdminRequest(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("requestId") Long requestId
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        firstCompanyAdminRequestService.rejectRequest(principal.getUserId(), requestId);
        return "redirect:/admin/companies";
    }

    @PostMapping("/users/status")
    public String updateUserStatus(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("userId") Long userId,
            @RequestParam("status") String status
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        platformAdminService.updateUserStatus(principal.getUserId(), userId, status);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/purchasing-role")
    public String updateUserPurchasingRole(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("userId") Long userId,
            @RequestParam("roleName") String roleName
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        platformAdminService.updateUserPurchasingRole(principal.getUserId(), userId, roleName);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/company-admin")
    public String updateUserCompanyAdmin(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("userId") Long userId,
            @RequestParam("companyAdmin") boolean companyAdmin
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        platformAdminService.updateUserCompanyAdmin(principal.getUserId(), userId, companyAdmin);
        return "redirect:/admin/users";
    }

    @PostMapping("/companies/status")
    public String updateCompanyStatus(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("companyId") Long companyId,
            @RequestParam("status") String status
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        platformAdminService.updateCompanyStatus(principal.getUserId(), companyId, status);
        return "redirect:/admin/companies";
    }

    @PostMapping("/orders/approve")
    public String approvePurchaseOrder(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("purchaseOrderId") Long purchaseOrderId,
            @RequestParam(name = "reviewMemo", required = false) String reviewMemo,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        platformAdminService.approvePurchaseOrder(principal.getUserId(), purchaseOrderId, reviewMemo);
        redirectAttributes.addFlashAttribute("adminAlert", "주문을 승인하고 결제 대기 상태로 전환했습니다.");
        return "redirect:/admin/orders?orderFilter=approved";
    }

    @PostMapping("/orders/reject")
    public String rejectPurchaseOrder(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("purchaseOrderId") Long purchaseOrderId,
            @RequestParam(name = "reviewMemo", required = false) String reviewMemo,
            @RequestParam("rejectionReason") String rejectionReason,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        platformAdminService.rejectPurchaseOrder(principal.getUserId(), purchaseOrderId, reviewMemo, rejectionReason);
        redirectAttributes.addFlashAttribute("adminAlert", "주문을 반려했습니다.");
        return "redirect:/admin/orders?orderFilter=rejected";
    }

    @PostMapping("/orders/ready-to-ship")
    public String markOrderReadyToShip(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("purchaseOrderId") Long purchaseOrderId,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        platformAdminService.markOrderReadyToShip(principal.getUserId(), purchaseOrderId);
        redirectAttributes.addFlashAttribute("adminAlert", "주문을 배송 준비 상태로 전환했습니다.");
        return "redirect:/admin/orders?orderFilter=delivery";
    }

    @PostMapping("/orders/ship")
    public String markOrderShipped(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("purchaseOrderId") Long purchaseOrderId,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        platformAdminService.markOrderShipped(principal.getUserId(), purchaseOrderId);
        redirectAttributes.addFlashAttribute("adminAlert", "주문을 배송 중 상태로 전환했습니다.");
        return "redirect:/admin/orders?orderFilter=delivery";
    }

    @PostMapping("/orders/deliver")
    public String markOrderDelivered(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("purchaseOrderId") Long purchaseOrderId,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        platformAdminService.markOrderDelivered(principal.getUserId(), purchaseOrderId);
        redirectAttributes.addFlashAttribute("adminAlert", "주문을 배송 완료 상태로 전환했습니다.");
        return "redirect:/admin/orders?orderFilter=delivery";
    }

    @PostMapping("/settlements/settle")
    public String markOrderSettled(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam("purchaseOrderId") Long purchaseOrderId,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        platformAdminService.markOrderSettled(principal.getUserId(), purchaseOrderId);
        redirectAttributes.addFlashAttribute("adminAlert", "선택한 주문의 정산을 완료했습니다.");
        return "redirect:/admin/settlements";
    }

    @PostMapping("/settlements/settle-bulk")
    public String markOrdersSettled(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(name = "purchaseOrderIds", required = false) java.util.List<Long> purchaseOrderIds,
            RedirectAttributes redirectAttributes
    ) {
        if (principal == null) {
            return "redirect:/";
        }
        if (purchaseOrderIds == null || purchaseOrderIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("adminAlert", "정산할 주문을 하나 이상 선택해 주세요.");
            return "redirect:/admin/settlements";
        }
        platformAdminService.markOrdersSettled(principal.getUserId(), purchaseOrderIds);
        redirectAttributes.addFlashAttribute("adminAlert", "선택한 주문들의 정산을 완료했습니다.");
        return "redirect:/admin/settlements";
    }

    private String validatePlatformAdmin(AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            return "redirect:/";
        }
        if (!platformAdminPageService.isPlatformAdmin(principal.getUserId())) {
            return "redirect:/workspace";
        }
        return null;
    }

    private void populateAdminPageModel(Model model, PlatformAdminPageView pageView) {
        model.addAttribute("workspaceUser", pageView.workspaceUser());
        model.addAttribute("activeSection", pageView.activeSection());
        model.addAttribute("activeOrderFilter", pageView.activeOrderFilter());
        model.addAttribute("pendingCompanyRegistrationRequests", pageView.pendingCompanyRegistrationRequests());
        model.addAttribute("pendingFirstCompanyAdminRequests", pageView.pendingFirstCompanyAdminRequests());
        model.addAttribute("platformAdminCompanies", pageView.platformAdminCompanies());
        model.addAttribute("platformAdminUsers", pageView.platformAdminUsers());
        model.addAttribute("purchasingRoleOptions", pageView.purchasingRoleOptions());
        model.addAttribute("platformApprovalOrders", pageView.platformApprovalOrders());
        model.addAttribute("settlementSummary", pageView.settlementSummary());
        model.addAttribute("platformSettlementOrders", pageView.platformSettlementOrders());
    }
}
