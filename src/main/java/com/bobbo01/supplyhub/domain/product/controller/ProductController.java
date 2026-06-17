package com.bobbo01.supplyhub.domain.product.controller;

import com.bobbo01.supplyhub.domain.product.service.ProductCatalogService;
import jakarta.servlet.http.HttpServletRequest;
import com.bobbo01.supplyhub.global.auth.oauth.AuthenticatedUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductCatalogService productCatalogService;

    public ProductController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    @GetMapping
    public String products(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                           @RequestParam(defaultValue = ProductCatalogService.ALL_CATEGORIES) String category,
                           @RequestParam(defaultValue = "") String q,
                           @RequestParam(defaultValue = "1") Integer page,
                           HttpServletRequest request,
                           Model model) {
        var catalogPage = productCatalogService.getCatalogPage(category, q, page);
        model.addAttribute("user", principal);
        model.addAttribute("isPlatformAdmin", isPlatformAdmin(principal));
        model.addAttribute("catalogPage", catalogPage);

        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return "pages/products :: catalogPageContent";
        }

        return "pages/products";
    }

    @GetMapping("/{productId}")
    public String productDetail(@PathVariable Long productId,
                                @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                Model model) {
        model.addAttribute("user", principal);
        model.addAttribute("isPlatformAdmin", isPlatformAdmin(principal));
        model.addAttribute("product", productCatalogService.getActiveProduct(productId));
        return "pages/product-detail";
    }

    private boolean isPlatformAdmin(AuthenticatedUserPrincipal principal) {
        return principal != null && principal.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_PLATFORM_ADMIN".equals(authority.getAuthority()));
    }
}
