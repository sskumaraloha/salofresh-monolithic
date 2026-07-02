package com.salofresh.security;

import com.salofresh.common.enums.TokenType;
import com.salofresh.config.AppProperties;
import com.salofresh.constant.SecurityConstants;
import com.salofresh.exception.TokenRefreshException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final AppProperties appProperties;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(appProperties.getJwt().getSecret().getBytes());
    }

    public String generateAccessToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + appProperties.getJwt().getAccessTokenExpirationMs());
        List<String> roles = principal.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toList());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(principal.getId()))
                .issuer(appProperties.getJwt().getIssuer())
                .claim(SecurityConstants.CLAIM_EMAIL, principal.getEmail())
                .claim(SecurityConstants.CLAIM_ROLES, roles)
                .claim(SecurityConstants.CLAIM_TOKEN_TYPE, TokenType.ACCESS.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshTokenValue(Long userId, boolean rememberMe) {
        Date now = new Date();
        long ttl = rememberMe
                ? appProperties.getJwt().getRememberMeRefreshTokenExpirationMs()
                : appProperties.getJwt().getRefreshTokenExpirationMs();
        Date expiry = new Date(now.getTime() + ttl);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .issuer(appProperties.getJwt().getIssuer())
                .claim(SecurityConstants.CLAIM_TOKEN_TYPE, TokenType.REFRESH.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("Expired JWT token: {}", ex.getMessage());
        } catch (MalformedJwtException | UnsupportedJwtException ex) {
            log.debug("Invalid JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.debug("JWT claims string is empty: {}", ex.getMessage());
        } catch (io.jsonwebtoken.security.SignatureException ex) {
            log.debug("Invalid JWT signature: {}", ex.getMessage());
        }
        return false;
    }

    public Long getUserIdFromToken(String token) {
        try {
            return Long.valueOf(parseClaims(token).getSubject());
        } catch (Exception ex) {
            throw new TokenRefreshException("Unable to extract user from token");
        }
    }

    public String getTokenId(String token) {
        return parseClaims(token).getId();
    }

    public Date getExpiryDate(String token) {
        return parseClaims(token).getExpiration();
    }

    public long getAccessTokenExpirationMs() {
        return appProperties.getJwt().getAccessTokenExpirationMs();
    }

    public long getRefreshTokenExpirationMs(boolean rememberMe) {
        return rememberMe
                ? appProperties.getJwt().getRememberMeRefreshTokenExpirationMs()
                : appProperties.getJwt().getRefreshTokenExpirationMs();
    }
}
