package com.salofresh.security;

import com.salofresh.config.AppProperties;
import com.salofresh.entity.User;
import com.salofresh.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenIssuanceService tokenIssuanceService;
    private final UserRepository userRepository;
    private final AppProperties appProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException("Authenticated OAuth2 user could not be found"));

        TokenPair tokenPair = tokenIssuanceService.issueTokens(user, true, request);

        String targetUrl = UriComponentsBuilder.fromUriString(appProperties.getFrontendUrl() + "/oauth2/redirect")
                .queryParam("accessToken", tokenPair.accessToken())
                .queryParam("refreshToken", tokenPair.refreshToken())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
