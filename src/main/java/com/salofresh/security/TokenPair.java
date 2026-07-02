package com.salofresh.security;

public record TokenPair(String accessToken, String refreshToken, long accessTokenExpiresInMs) {
}
