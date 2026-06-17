package com.bobbo01.supplyhub.global.auth.oauth;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.List;
import java.util.Map;

// Provider별 속성 형식을 공통 구조로 정리한 값 객체.
// 각 Provider마다 키 이름이 조금씩 달라도 서비스 계층에서는
// 같은 형태의 attributes로 처리할 수 있게 변환한다.
public record OAuth2Attributes(
        String registrationId,
        String providerUserId,
        String email,
        boolean emailVerified,
        String name,
        String phone,
        String username,
        Map<String, Object> attributes,
        String nameAttributeKey
) {

    public static OAuth2Attributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        // 새로운 Provider를 붙일 때 서비스 로직을 수정하기보다
        // 여기서 claim 우선순위만 조정하는 방식으로 대응할 수 있다.
        String resolvedNameAttributeKey = resolveNameAttributeKey(userNameAttributeName);
        String providerUserId = firstNonBlank(
                stringValue(attributes.get("sub")),
                stringValue(attributes.get(resolvedNameAttributeKey)),
                stringValue(attributes.get("id")),
                stringValue(attributes.get("uid"))
        );
        String email = firstNonBlank(
                stringValue(attributes.get("email")),
                stringValue(attributes.get("preferred_username")),
                stringValue(attributes.get("upn"))
        );
        String name = firstNonBlank(
                stringValue(attributes.get("name")),
                stringValue(attributes.get("display_name")),
                stringValue(attributes.get("displayName")),
                stringValue(attributes.get("given_name"))
        );
        String phone = firstNonBlank(
                stringValue(attributes.get("phone_number")),
                stringValue(attributes.get("phone")),
                stringValue(attributes.get("mobile"))
        );
        String username = firstNonBlank(
                stringValue(attributes.get("preferred_username")),
                stringValue(attributes.get("upn")),
                stringValue(attributes.get("login")),
                email
        );

        return new OAuth2Attributes(
                registrationId,
                providerUserId,
                email,
                resolveEmailVerified(attributes),
                name,
                phone,
                username,
                attributes,
                resolvedNameAttributeKey
        );
    }

    public String resolvedName() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        if (username != null && !username.isBlank()) {
            return username;
        }
        if (email != null && !email.isBlank()) {
            return email;
        }
        return registrationId + "_" + providerUserId;
    }

    public void validateRequiredFields() {
        if (providerUserId == null || providerUserId.isBlank()) {
            throw invalidAttributes(registrationId, "Provider user identifier is missing.");
        }
        if (email == null || email.isBlank()) {
            throw invalidAttributes(registrationId, "Provider did not return an email or username claim.");
        }
    }

    private static String resolveNameAttributeKey(String userNameAttributeName) {
        if (userNameAttributeName != null && !userNameAttributeName.isBlank()) {
            return userNameAttributeName;
        }
        return "sub";
    }

    private static boolean resolveEmailVerified(Map<String, Object> attributes) {
        for (String key : List.of("email_verified", "verified_email")) {
            Object value = attributes.get(key);
            if (value != null) {
                return booleanValue(value);
            }
        }
        return false;
    }

    private static OAuth2AuthenticationException invalidAttributes(String registrationId, String message) {
        return new OAuth2AuthenticationException(
                new OAuth2Error("invalid_user_info"),
                registrationId + ": " + message
        );
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }
}

