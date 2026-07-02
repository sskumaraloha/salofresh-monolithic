package com.salofresh.security;

import com.salofresh.common.enums.AuthProvider;
import com.salofresh.common.enums.RoleName;
import com.salofresh.entity.Role;
import com.salofresh.entity.User;
import com.salofresh.exception.BadRequestException;
import com.salofresh.repository.RoleRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.util.RandomCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        GoogleOAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(oAuth2User.getAttributes());

        if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            throw new OAuth2AuthenticationException("Email not found from Google OAuth2 provider");
        }

        User user = userRepository.findByEmailIgnoreCaseAndDeletedFalse(userInfo.getEmail())
                .map(existing -> updateExistingUser(existing, userInfo))
                .orElseGet(() -> registerNewUser(userInfo));

        return new OAuth2UserPrincipal(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(GoogleOAuth2UserInfo userInfo) {
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new BadRequestException("Default CUSTOMER role is not configured"));

        User user = User.builder()
                .email(userInfo.getEmail())
                .firstName(userInfo.getFirstName())
                .lastName(userInfo.getLastName())
                .profileImageUrl(userInfo.getImageUrl())
                .authProvider(AuthProvider.GOOGLE)
                .providerId(userInfo.getId())
                .emailVerified(userInfo.isEmailVerified())
                .referralCode(generateUniqueReferralCode())
                .roles(Set.of(customerRole))
                .build();
        return userRepository.save(user);
    }

    private User updateExistingUser(User existing, GoogleOAuth2UserInfo userInfo) {
        existing.setFirstName(userInfo.getFirstName());
        if (userInfo.getLastName() != null) {
            existing.setLastName(userInfo.getLastName());
        }
        if (existing.getProfileImageUrl() == null) {
            existing.setProfileImageUrl(userInfo.getImageUrl());
        }
        existing.setLastLoginAt(Instant.now());
        return userRepository.save(existing);
    }

    private String generateUniqueReferralCode() {
        String code;
        do {
            code = RandomCodeGenerator.generateAlphanumeric(8);
        } while (userRepository.existsByReferralCode(code));
        return code;
    }
}
