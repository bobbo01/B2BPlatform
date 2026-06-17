package com.bobbo01.supplyhub.global.auth.oauth;

import com.bobbo01.supplyhub.domain.role.entity.RoleNames;
import com.bobbo01.supplyhub.domain.user.entity.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class UserAuthorities {

    private static final Set<String> PURCHASING_ROLE_NAMES = Set.of(
            RoleNames.CART_USER,
            RoleNames.PURCHASER,
            RoleNames.APPROVER
    );

    private UserAuthorities() {
    }

    public static List<SimpleGrantedAuthority> asSimpleGrantedAuthorities(User user) {
        return authorityNames(user).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    public static List<String> authorityNames(User user) {
        LinkedHashSet<String> authorities = new LinkedHashSet<>();
        if (user.isPlatformAdmin()) {
            authorities.add("ROLE_PLATFORM_ADMIN");
            return List.copyOf(authorities);
        }

        if (!user.isCompanyUser()) {
            return List.copyOf(authorities);
        }

        if (user.hasAnyPurchasingRole(PURCHASING_ROLE_NAMES.toArray(String[]::new))) {
            authorities.add("ROLE_" + user.getPurchasingRoleName());
        }

        if (user.hasCompanyAdminRole()) {
            authorities.add("ROLE_COMPANY_ADMIN");
        }

        return List.copyOf(authorities);
    }
}
