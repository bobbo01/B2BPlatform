package com.bobbo01.supplyhub.domain.home.service;

import com.bobbo01.supplyhub.domain.company.entity.Company;
import com.bobbo01.supplyhub.domain.product.dto.ProductSummaryView;
import com.bobbo01.supplyhub.domain.product.service.ProductCatalogService;
import com.bobbo01.supplyhub.domain.home.dto.SsoProviderView;
import com.bobbo01.supplyhub.domain.home.dto.WorkspaceCompanyView;
import com.bobbo01.supplyhub.domain.home.dto.WorkspaceUserView;
import com.bobbo01.supplyhub.domain.role.RoleLabeler;
import com.bobbo01.supplyhub.domain.role.entity.Role;
import com.bobbo01.supplyhub.domain.user.entity.User;
import com.bobbo01.supplyhub.domain.user.repository.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class HomeService {

    private static final int HOME_FEATURED_PRODUCT_LIMIT = 6;

    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;
    private final ProductCatalogService productCatalogService;
    private final UserRepository userRepository;

    public HomeService(
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider,
            ProductCatalogService productCatalogService,
            UserRepository userRepository
    ) {
        this.clientRegistrationRepositoryProvider = clientRegistrationRepositoryProvider;
        this.productCatalogService = productCatalogService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<SsoProviderView> getSsoProviders() {
        ClientRegistrationRepository repository = clientRegistrationRepositoryProvider.getIfAvailable();
        if (!(repository instanceof Iterable<?> iterable)) {
            return List.of();
        }

        List<SsoProviderView> providers = new ArrayList<>();
        for (Object candidate : iterable) {
            if (candidate instanceof ClientRegistration registration) {
                providers.add(new SsoProviderView(
                        registration.getRegistrationId(),
                        registration.getClientName(),
                        "/oauth2/authorization/" + registration.getRegistrationId()
                ));
            }
        }
        return providers;
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryView> getFeaturedProducts() {
        return productCatalogService.getFeaturedProducts(HOME_FEATURED_PRODUCT_LIMIT);
    }

    @Transactional(readOnly = true)
    public WorkspaceUserView getWorkspaceUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user could not be found."));
        if (user.isPlatformAdmin()) {
            return new WorkspaceUserView(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    true,
                    RoleLabeler.toRoleLabel("PLATFORM_ADMIN"),
                    false,
                    null
            );
        }

        Company company = user.getCompany();
        return new WorkspaceUserView(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                false,
                roleLabel(user),
                user.hasCompanyAdminRole(),
                new WorkspaceCompanyView(
                        company.getId(),
                        company.getCompanyName(),
                        company.getCompanyDomain(),
                        company.getInviteCode(),
                        company.getStatus()
                )
        );
    }

    @Transactional(readOnly = true)
    public boolean isLinkedUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user could not be found."));
        return user.isPlatformAdmin() || user.getCompany() != null;
    }

    @Transactional(readOnly = true)
    public String getLinkedCompanyName(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user could not be found."));
        return user.getCompany() != null ? user.getCompany().getCompanyName() : null;
    }

    private String roleLabel(User user) {
        Role purchasingRole = user.getPurchasingRole();
        return RoleLabeler.toRoleLabel(purchasingRole != null ? purchasingRole.getRoleName() : null);
    }
}

