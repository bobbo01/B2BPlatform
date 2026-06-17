package com.bobbo01.supplyhub.global.auth;

import com.bobbo01.supplyhub.global.auth.oauth.OAuth2LoginSuccessHandler;
import com.bobbo01.supplyhub.global.auth.oauth.CustomOidcUserService;
import com.bobbo01.supplyhub.global.auth.oauth.CustomOAuth2UserService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AuthConfig {

    private final CustomOidcUserService customOidcUserService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;

    public AuthConfig(
            CustomOidcUserService customOidcUserService,
            CustomOAuth2UserService customOAuth2UserService,
            OAuth2LoginSuccessHandler oauth2LoginSuccessHandler
    ) {
        this.customOidcUserService = customOidcUserService;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oauth2LoginSuccessHandler = oauth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider
    ) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/error", "/css/**", "/js/**", "/images/**", "/workspace", "/company/first-login", "/products", "/products/**").permitAll()
                        .requestMatchers("/admin", "/admin/**").hasRole("PLATFORM_ADMIN")
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout.logoutSuccessUrl("/"));

        if (clientRegistrationRepositoryProvider.getIfAvailable() != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService)
                            .oidcUserService(customOidcUserService)
                    )
                    .successHandler(oauth2LoginSuccessHandler)
            );
        }

        return http.build();
    }
}
