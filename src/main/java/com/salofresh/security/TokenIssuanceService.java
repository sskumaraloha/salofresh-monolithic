package com.salofresh.security;

import com.salofresh.entity.RefreshToken;
import com.salofresh.entity.User;
import com.salofresh.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenIssuanceService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public TokenPair issueTokens(User user, boolean rememberMe, HttpServletRequest request) {
        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshTokenValue = jwtTokenProvider.generateRefreshTokenValue(user.getId(), rememberMe);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiryDate(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs(rememberMe)))
                .rememberMe(rememberMe)
                .deviceInfo(request == null ? null : request.getHeader("User-Agent"))
                .ipAddress(request == null ? null : resolveClientIp(request))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return new TokenPair(accessToken, refreshTokenValue, jwtTokenProvider.getAccessTokenExpirationMs());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
