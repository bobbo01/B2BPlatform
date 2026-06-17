package com.bobbo01.supplyhub.global.auth.oauth;

import com.bobbo01.supplyhub.domain.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Collection;

public class CustomOidcUser extends DefaultOidcUser {

    private final Long userId;
    private final String email;
    private final OAuth2Attributes oauth2Attributes;

    private CustomOidcUser(
            Collection<? extends GrantedAuthority> authorities,
            OidcIdToken idToken,
            OidcUserInfo userInfo,
            String nameAttributeKey,
            Long userId,
            String email,
            OAuth2Attributes oauth2Attributes
    ) {
        super(authorities, idToken, userInfo, nameAttributeKey);
        this.userId = userId;
        this.email = email;
        this.oauth2Attributes = oauth2Attributes;
    }

    public static CustomOidcUser pending(
            Collection<? extends GrantedAuthority> authorities,
            OidcIdToken idToken,
            OidcUserInfo userInfo,
            String nameAttributeKey,
            OAuth2Attributes oauth2Attributes
    ) {
        return new CustomOidcUser(authorities, idToken, userInfo, nameAttributeKey, null, oauth2Attributes.email(), oauth2Attributes);
    }

    public static CustomOidcUser linked(
            Collection<? extends GrantedAuthority> authorities,
            OidcIdToken idToken,
            OidcUserInfo userInfo,
            String nameAttributeKey,
            OAuth2Attributes oauth2Attributes,
            User user
    ) {
        return new CustomOidcUser(authorities, idToken, userInfo, nameAttributeKey, user.getId(), user.getEmail(), oauth2Attributes);
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public OAuth2Attributes getOauth2Attributes() {
        return oauth2Attributes;
    }

    public boolean hasLinkedUser() {
        return userId != null;
    }
}
