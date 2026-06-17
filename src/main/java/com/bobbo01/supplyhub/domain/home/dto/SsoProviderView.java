package com.bobbo01.supplyhub.domain.home.dto;

public record SsoProviderView(
        String registrationId,
        String clientName,
        String authorizationPath
) {
}

