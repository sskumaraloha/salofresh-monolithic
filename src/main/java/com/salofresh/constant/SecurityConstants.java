package com.salofresh.constant;

public final class SecurityConstants {

    private SecurityConstants() {
    }

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String CLAIM_EMAIL = "email";

    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/api/v1/public/**",
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/actuator/info",
            "/oauth2/**",
            "/login/oauth2/**",
            "/files/**",
            "/api/v1/categories/**",
            "/api/v1/services/**",
            "/api/v1/salons/*/services/**",
            "/api/v1/salons/*/reviews",
            "/api/v1/employees/*/reviews",
            "/api/v1/salons/*/employees",
            "/api/v1/salons/*/employees/*",
            "/api/v1/salons/*/employees/*/schedule",
            "/api/v1/salons/*/slots",
            "/api/v1/salons",
            "/api/v1/salons/*",
            "/api/v1/locations/**",
            "/api/v1/guest/**"
    };
}
