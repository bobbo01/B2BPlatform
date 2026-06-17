package com.bobbo01.supplyhub.global.auth.oauth;

import com.bobbo01.supplyhub.domain.user.entity.User;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.List;

public class AuthenticatedUserPrincipal implements Principal, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String email;
    private final String displayName;
    private final List<? extends GrantedAuthority> authorities;

    private AuthenticatedUserPrincipal(Long userId, String email, String displayName, List<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.authorities = List.copyOf(authorities);
    }

    public static AuthenticatedUserPrincipal from(User user, Collection<? extends GrantedAuthority> authorities) {
        return new AuthenticatedUserPrincipal(user.getId(), user.getEmail(), user.getFullName(), List.copyOf(authorities));
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return email;
    }
}
