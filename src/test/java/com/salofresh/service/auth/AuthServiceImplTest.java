package com.salofresh.service.auth;

import com.salofresh.common.enums.AccountStatus;
import com.salofresh.common.enums.AuthProvider;
import com.salofresh.common.enums.RoleName;
import com.salofresh.config.AppProperties;
import com.salofresh.dto.auth.LoginRequest;
import com.salofresh.dto.auth.LoginResponse;
import com.salofresh.dto.auth.RegisterCustomerRequest;
import com.salofresh.dto.auth.UserSummaryResponse;
import com.salofresh.entity.Customer;
import com.salofresh.entity.Role;
import com.salofresh.entity.User;
import com.salofresh.exception.AccountLockedException;
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
import com.salofresh.service.impl.auth.AuthServiceImpl;
import com.salofresh.wallet.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SalonOwnerRepository salonOwnerRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private BlacklistedTokenRepository blacklistedTokenRepository;
    @Mock
    private LoginHistoryRepository loginHistoryRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private WalletService walletService;
    @Mock
    private OtpService otpService;
    @Mock
    private TokenIssuanceService tokenIssuanceService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AppProperties appProperties;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getSecurity().setMaxFailedLoginAttempts(5);
        appProperties.getSecurity().setAccountLockDurationMinutes(30);

        authService = new AuthServiceImpl(userRepository, roleRepository, customerRepository, salonOwnerRepository,
                refreshTokenRepository, blacklistedTokenRepository, loginHistoryRepository, passwordEncoder,
                walletService, otpService, tokenIssuanceService, jwtTokenProvider, userMapper, eventPublisher,
                appProperties);
    }

    private User buildUser() {
        return User.builder()
                .id(1L)
                .email("jane@example.com")
                .phone("9876543210")
                .password("encoded-password")
                .firstName("Jane")
                .lastName("Doe")
                .authProvider(AuthProvider.LOCAL)
                .accountStatus(AccountStatus.ACTIVE)
                .failedLoginAttempts(0)
                .build();
    }

    @Test
    void registerCustomer_createsUserWithWalletAndSendsOtp() {
        RegisterCustomerRequest request = RegisterCustomerRequest.builder()
                .email("new@example.com")
                .phone("9123456789")
                .password("StrongP@ss1")
                .firstName("New")
                .lastName("User")
                .build();

        when(userRepository.existsByEmailIgnoreCase(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(false);
        when(roleRepository.findByName(RoleName.CUSTOMER))
                .thenReturn(Optional.of(Role.builder().id(1L).name(RoleName.CUSTOMER).build()));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");
        when(userRepository.existsByReferralCode(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toSummary(any(User.class))).thenReturn(UserSummaryResponse.builder()
                .email(request.getEmail()).firstName(request.getFirstName()).build());

        UserSummaryResponse response = authService.registerCustomer(request);

        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        verify(walletService).getOrCreateWallet(any(User.class));
        verify(otpService).generateAndSend(eq(request.getEmail()), eq(request.getFirstName()), any(), any());
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void login_withCorrectCredentials_returnsTokensAndRecordsHistory() {
        User user = buildUser();
        LoginRequest request = LoginRequest.builder().identifier("jane@example.com").password("Password1!").build();
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(userRepository.findByEmailIgnoreCaseOrPhoneAndDeletedFalse(request.getIdentifier(), request.getIdentifier()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
        when(tokenIssuanceService.issueTokens(eq(user), eq(false), eq(httpRequest)))
                .thenReturn(new TokenPair("access-token", "refresh-token", 900000L));
        when(userMapper.toSummary(user)).thenReturn(UserSummaryResponse.builder().email(user.getEmail()).build());

        LoginResponse response = authService.login(request, httpRequest);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(user.getFailedLoginAttempts()).isZero();
        verify(loginHistoryRepository).save(argThatSuccess());
    }

    @Test
    void login_withWrongPassword_incrementsFailedAttempts() {
        User user = buildUser();
        LoginRequest request = LoginRequest.builder().identifier("jane@example.com").password("WrongPass1!").build();
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(userRepository.findByEmailIgnoreCaseOrPhoneAndDeletedFalse(request.getIdentifier(), request.getIdentifier()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(UnauthorizedException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        verify(loginHistoryRepository).save(any());
    }

    @Test
    void login_afterMaxFailedAttempts_locksAccountOnSubsequentAttempt() {
        User user = buildUser();
        user.setFailedLoginAttempts(4);
        LoginRequest request = LoginRequest.builder().identifier("jane@example.com").password("WrongPass1!").build();
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(userRepository.findByEmailIgnoreCaseOrPhoneAndDeletedFalse(request.getIdentifier(), request.getIdentifier()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(UnauthorizedException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getAccountLockedUntil()).isNotNull();
        assertThat(user.isAccountLocked()).isTrue();
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void login_whenAccountAlreadyLocked_throwsAccountLockedException() {
        User user = buildUser();
        user.setAccountLockedUntil(Instant.now().plusSeconds(600));
        LoginRequest request = LoginRequest.builder().identifier("jane@example.com").password("Password1!").build();
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(userRepository.findByEmailIgnoreCaseOrPhoneAndDeletedFalse(request.getIdentifier(), request.getIdentifier()))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(AccountLockedException.class);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(loginHistoryRepository, times(1)).save(any());
    }

    private com.salofresh.entity.LoginHistory argThatSuccess() {
        return org.mockito.ArgumentMatchers.argThat(history -> history.isSuccess());
    }
}
