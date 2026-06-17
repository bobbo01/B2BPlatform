package com.bobbo01.supplyhub.global.auth.oauth;

import com.bobbo01.supplyhub.domain.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User extends DefaultOAuth2User {

    private final Long userId;
    private final String email;
    private final OAuth2Attributes oauth2Attributes;

    private CustomOAuth2User(
            Collection<? extends GrantedAuthority> authorities,
            Map<String, Object> attributes,
            String nameAttributeKey,
            Long userId,
            String email,
            OAuth2Attributes oauth2Attributes
    ) {
        super(authorities, attributes, nameAttributeKey);
        this.userId = userId;
        this.email = email;
        this.oauth2Attributes = oauth2Attributes;
    }

    public static CustomOAuth2User pending(
            Collection<? extends GrantedAuthority> authorities,
            Map<String, Object> attributes,
            String nameAttributeKey,
            OAuth2Attributes oauth2Attributes
    ) {
        return new CustomOAuth2User(authorities, attributes, nameAttributeKey, null, oauth2Attributes.email(), oauth2Attributes);
    }

    public static CustomOAuth2User linked(
            Collection<? extends GrantedAuthority> authorities,
            Map<String, Object> attributes,
            String nameAttributeKey,
            OAuth2Attributes oauth2Attributes,
            User user
    ) {
        return new CustomOAuth2User(authorities, attributes, nameAttributeKey, user.getId(), user.getEmail(), oauth2Attributes);
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
