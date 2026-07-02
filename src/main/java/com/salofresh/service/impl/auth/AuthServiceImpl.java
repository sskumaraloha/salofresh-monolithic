package com.salofresh.service.impl.auth;

import com.salofresh.common.enums.OtpChannel;
import com.salofresh.common.enums.OtpType;
import com.salofresh.common.enums.RoleName;
import com.salofresh.common.enums.TokenType;
import com.salofresh.constant.AppConstants;
import com.salofresh.constant.SecurityConstants;
import com.salofresh.config.AppProperties;
import com.salofresh.dto.auth.ChangePasswordRequest;
import com.salofresh.dto.auth.ForgotPasswordRequest;
import com.salofresh.dto.auth.LoginRequest;
import com.salofresh.dto.auth.LoginResponse;
import com.salofresh.dto.auth.RegisterCustomerRequest;
import com.salofresh.dto.auth.RegisterSalonOwnerRequest;
import com.salofresh.dto.auth.ResetPasswordRequest;
import com.salofresh.dto.auth.UserSummaryResponse;
import com.salofresh.entity.BlacklistedToken;
import com.salofresh.entity.Customer;
import com.salofresh.entity.LoginHistory;
import com.salofresh.entity.RefreshToken;
import com.salofresh.entity.Role;
import com.salofresh.entity.SalonOwner;
import com.salofresh.entity.User;
import com.salofresh.event.AccountLockedEvent;
import com.salofresh.event.UserRegisteredEvent;
import com.salofresh.exception.AccountLockedException;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ConflictException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.exception.TokenRefreshException;
import com.salofresh.exception.UnauthorizedException;
import com.salofresh.mapper.auth.UserMapper;
import com.salofresh.otp.OtpService;
import com.salofresh.repository.BlacklistedTokenRepository;
import com.salofresh.repository.CustomerRepository;
import com.salofresh.repository.LoginHistoryRepository;
import com.salofresh.repository.RefreshTokenRepository;
import com.salofresh.repository.RoleRepository;
import com.salofresh.repository.SalonOwnerRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.security.JwtTokenProvider;
import com.salofresh.security.TokenIssuanceService;
import com.salofresh.security.TokenPair;
import com.salofresh.service.auth.AuthService;
import com.salofresh.util.RandomCodeGenerator;
import com.salofresh.wallet.WalletService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final SalonOwnerRepository salonOwnerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;
    private final OtpService otpService;
    private final TokenIssuanceService tokenIssuanceService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public UserSummaryResponse registerCustomer(RegisterCustomerRequest request) {
        validateEmailAndPhoneAvailable(request.getEmail(), request.getPhone());

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", RoleName.CUSTOMER));

        String referredByCode = null;
        if (request.getReferralCode() != null && !request.getReferralCode().isBlank()) {
            userRepository.findByReferralCode(request.getReferralCode())
                    .orElseThrow(() -> new BadRequestException("Invalid referral code"));
            referredByCode = request.getReferralCode();
        }

        User user = User.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .referralCode(generateUniqueReferralCode())
                .referredByCode(referredByCode)
                .roles(Set.of(customerRole))
                .build();
        userRepository.save(user);

        Customer customer = Customer.builder()
                .user(user)
                .build();
        customerRepository.save(customer);

        walletService.getOrCreateWallet(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(user));
        otpService.generateAndSend(user.getEmail(), user.getFirstName(), OtpType.EMAIL_VERIFICATION, OtpChannel.EMAIL);

        return userMapper.toSummary(user);
    }

    @Override
    @Transactional
    public UserSummaryResponse registerSalonOwner(RegisterSalonOwnerRequest request) {
        validateEmailAndPhoneAvailable(request.getEmail(), request.getPhone());

        Role salonOwnerRole = roleRepository.findByName(RoleName.SALON_OWNER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", RoleName.SALON_OWNER));

        User user = User.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .referralCode(generateUniqueReferralCode())
                .roles(Set.of(salonOwnerRole))
                .build();
        userRepository.save(user);

        SalonOwner salonOwner = SalonOwner.builder()
                .user(user)
                .businessName(request.getBusinessName())
                .build();
        salonOwnerRepository.save(salonOwner);

        walletService.getOrCreateWallet(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(user));
        otpService.generateAndSend(user.getEmail(), user.getFirstName(), OtpType.EMAIL_VERIFICATION, OtpChannel.EMAIL);

        return userMapper.toSummary(user);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmailIgnoreCaseOrPhoneAndDeletedFalse(request.getIdentifier(), request.getIdentifier())
                .orElseThrow(() -> new UnauthorizedException("Invalid email/phone or password"));

        if (user.isAccountLocked()) {
            recordLoginHistory(user, httpRequest, false, "Account is locked");
            throw new AccountLockedException("Account is locked due to multiple failed login attempts. Please try again later.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLogin(user);
            recordLoginHistory(user, httpRequest, false, "Invalid password");
            throw new UnauthorizedException("Invalid email/phone or password");
        }

        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        recordLoginHistory(user, httpRequest, true, null);

        TokenPair tokenPair = tokenIssuanceService.issueTokens(user, request.isRememberMe(), httpRequest);
        return buildLoginResponse(tokenPair, user);
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(String refreshTokenValue, HttpServletRequest httpRequest) {
        if (!jwtTokenProvider.validateToken(refreshTokenValue)) {
            throw new TokenRefreshException("Invalid or expired refresh token");
        }
        Claims claims = jwtTokenProvider.parseClaims(refreshTokenValue);
        String tokenType = claims.get(SecurityConstants.CLAIM_TOKEN_TYPE, String.class);
        if (!TokenType.REFRESH.name().equals(tokenType)) {
            throw new TokenRefreshException("Provided token is not a refresh token");
        }

        RefreshToken storedToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                .orElseThrow(() -> new TokenRefreshException("Refresh token not found or already revoked"));

        if (storedToken.isExpired()) {
            throw new TokenRefreshException("Refresh token has expired, please login again");
        }

        User user = storedToken.getUser();

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        TokenPair tokenPair = tokenIssuanceService.issueTokens(user, storedToken.isRememberMe(), httpRequest);
        return buildLoginResponse(tokenPair, user);
    }

    @Override
    @Transactional
    public void logout(String refreshTokenValue, String accessToken) {
        if (refreshTokenValue != null && !refreshTokenValue.isBlank()) {
            refreshTokenRepository.revokeByToken(refreshTokenValue);
        }

        if (accessToken != null && !accessToken.isBlank() && jwtTokenProvider.validateToken(accessToken)) {
            String jti = jwtTokenProvider.getTokenId(accessToken);
            if (!blacklistedTokenRepository.existsByTokenId(jti)) {
                BlacklistedToken blacklistedToken = BlacklistedToken.builder()
                        .tokenId(jti)
                        .expiryDate(jwtTokenProvider.getExpiryDate(accessToken).toInstant())
                        .build();
                blacklistedTokenRepository.save(blacklistedToken);
            }
        }
    }

    @Override
    @Transactional
    public void logoutAllDevices(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        refreshTokenRepository.revokeAllByUser(user);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCaseOrPhoneAndDeletedFalse(request.getIdentifier(), request.getIdentifier())
                .orElseThrow(() -> new ResourceNotFoundException("User", "identifier", request.getIdentifier()));

        OtpChannel channel = request.getIdentifier().contains("@") ? OtpChannel.EMAIL : OtpChannel.SMS;
        String recipient = channel == OtpChannel.EMAIL ? user.getEmail() : user.getPhone();
        otpService.generateAndSend(recipient, user.getFirstName(), OtpType.FORGOT_PASSWORD, channel);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCaseOrPhoneAndDeletedFalse(request.getIdentifier(), request.getIdentifier())
                .orElseThrow(() -> new ResourceNotFoundException("User", "identifier", request.getIdentifier()));

        String otpIdentifier = request.getIdentifier().contains("@") ? user.getEmail() : user.getPhone();
        otpService.verify(otpIdentifier, OtpType.FORGOT_PASSWORD, request.getOtp());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUser(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // NOTE: ChangePasswordRequest does not carry the caller's current refresh token,
        // so there is no reliable way to identify "this" session and exclude it. All
        // refresh tokens are revoked, forcing re-login on every device including this one.
        refreshTokenRepository.revokeAllByUser(user);
    }

    @Override
    @Transactional
    public void verifyOtp(String identifier, OtpType otpType, String otp) {
        otpService.verify(identifier, otpType, otp);

        if (otpType == OtpType.EMAIL_VERIFICATION || otpType == OtpType.PHONE_VERIFICATION) {
            User user = userRepository.findByEmailIgnoreCaseOrPhoneAndDeletedFalse(identifier, identifier)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "identifier", identifier));
            if (otpType == OtpType.EMAIL_VERIFICATION) {
                user.setEmailVerified(true);
            } else {
                user.setPhoneVerified(true);
            }
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public void resendOtp(String identifier, OtpType otpType) {
        otpService.resend(identifier, otpType);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return userMapper.toSummary(user);
    }

    private void validateEmailAndPhoneAvailable(String email, String phone) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new ConflictException("An account with this phone number already exists");
        }
    }

    private String generateUniqueReferralCode() {
        String code;
        do {
            code = RandomCodeGenerator.generateAlphanumeric(AppConstants.REFERRAL_CODE_LENGTH);
        } while (userRepository.existsByReferralCode(code));
        return code;
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= appProperties.getSecurity().getMaxFailedLoginAttempts()) {
            user.setAccountLockedUntil(Instant.now().plus(Duration.ofMinutes(appProperties.getSecurity().getAccountLockDurationMinutes())));
            userRepository.save(user);
            eventPublisher.publishEvent(new AccountLockedEvent(user));
        } else {
            userRepository.save(user);
        }
    }

    private void recordLoginHistory(User user, HttpServletRequest httpRequest, boolean success, String failureReason) {
        LoginHistory history = LoginHistory.builder()
                .user(user)
                .loginAt(Instant.now())
                .ipAddress(resolveClientIp(httpRequest))
                .userAgent(httpRequest == null ? null : httpRequest.getHeader("User-Agent"))
                .success(success)
                .failureReason(failureReason)
                .build();
        loginHistoryRepository.save(history);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private LoginResponse buildLoginResponse(TokenPair tokenPair, User user) {
        return LoginResponse.builder()
                .accessToken(tokenPair.accessToken())
                .refreshToken(tokenPair.refreshToken())
                .tokenType("Bearer")
                .expiresIn(tokenPair.accessTokenExpiresInMs())
                .user(userMapper.toSummary(user))
                .build();
    }
}
