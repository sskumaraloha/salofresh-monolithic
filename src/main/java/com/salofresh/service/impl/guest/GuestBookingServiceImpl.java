package com.salofresh.service.impl.guest;

import com.salofresh.common.enums.AuthProvider;
import com.salofresh.common.enums.RoleName;
import com.salofresh.constant.AppConstants;
import com.salofresh.dto.booking.AppointmentResponse;
import com.salofresh.dto.booking.AppointmentSummaryResponse;
import com.salofresh.dto.booking.CreateBookingRequest;
import com.salofresh.dto.guest.GuestBookingRequest;
import com.salofresh.dto.guest.GuestBookingResponse;
import com.salofresh.dto.guest.GuestLookupRequest;
import com.salofresh.entity.Appointment;
import com.salofresh.entity.AppointmentService;
import com.salofresh.entity.Customer;
import com.salofresh.entity.Role;
import com.salofresh.entity.User;
import com.salofresh.event.UserRegisteredEvent;
import com.salofresh.exception.ConflictException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.booking.AppointmentMapper;
import com.salofresh.repository.AppointmentRepository;
import com.salofresh.repository.AppointmentServiceRepository;
import com.salofresh.repository.CustomerRepository;
import com.salofresh.repository.RoleRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.security.TokenIssuanceService;
import com.salofresh.security.TokenPair;
import com.salofresh.security.UserPrincipal;
import com.salofresh.service.booking.BookingService;
import com.salofresh.service.guest.GuestBookingService;
import com.salofresh.util.RandomCodeGenerator;
import com.salofresh.wallet.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Thin guest-identity layer in front of the existing {@link BookingService}. It does not
 * re-implement any slot-validation, pricing, coupon, or payment logic - it only resolves (or
 * creates) a guest {@link User}/{@link Customer}/wallet, and then calls the exact same
 * {@code BookingService#createBooking(CreateBookingRequest)} that authenticated customers use.
 *
 * <p>Because {@code BookingServiceImpl#createBooking} reads the current customer via
 * {@code SecurityUtils#getCurrentUserId()} (i.e. from the Spring Security context) rather than as
 * an explicit method parameter, this service temporarily installs an {@link Authentication} for
 * the guest user before calling it, and restores whatever was there before in a finally block.
 * This is the same {@link UsernamePasswordAuthenticationToken}/{@link UserPrincipal} shape that
 * {@code JwtAuthenticationFilter} installs for a normal authenticated request.</p>
 */
@Service
@RequiredArgsConstructor
public class GuestBookingServiceImpl implements GuestBookingService {

    private static final String GUEST_EMAIL_DOMAIN = "guest.salofresh.local";

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentServiceRepository appointmentServiceRepository;
    private final AppointmentMapper appointmentMapper;
    private final WalletService walletService;
    private final BookingService bookingService;
    private final TokenIssuanceService tokenIssuanceService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public GuestBookingResponse createGuestBookingAndAccount(GuestBookingRequest request, HttpServletRequest httpRequest) {
        User guest = resolveOrCreateGuest(request);

        CreateBookingRequest bookingRequest = CreateBookingRequest.builder()
                .salonId(request.getSalonId())
                .employeeId(request.getEmployeeId())
                .appointmentDate(request.getAppointmentDate())
                .startTime(request.getStartTime())
                .serviceIds(request.getServiceIds())
                .couponCode(request.getCouponCode())
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .build();

        AppointmentResponse appointmentResponse = createBookingAsGuest(guest, bookingRequest);

        TokenPair tokenPair = tokenIssuanceService.issueTokens(guest, false, httpRequest);

        return GuestBookingResponse.builder()
                .bookingNumber(appointmentResponse.getBookingNumber())
                .accessToken(tokenPair.accessToken())
                .refreshToken(tokenPair.refreshToken())
                .tokenType("Bearer")
                .expiresIn(tokenPair.accessTokenExpiresInMs())
                .appointment(appointmentResponse)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentSummaryResponse lookupGuestBooking(GuestLookupRequest request) {
        User guest = userRepository.findByPhoneAndDeletedFalse(request.getPhone())
                .filter(user -> user.getAuthProvider() == AuthProvider.GUEST)
                .orElseThrow(GuestBookingServiceImpl::notFound);

        Appointment appointment = appointmentRepository.findByBookingNumber(request.getBookingNumber())
                .orElseThrow(GuestBookingServiceImpl::notFound);

        if (!appointment.getCustomer().getId().equals(guest.getId())) {
            throw notFound();
        }

        return toSummaryResponse(appointment);
    }

    /**
     * Looks up an existing guest identity by phone number, reusing it (per the spec: "if a guest
     * re-books later using the SAME phone number before ever setting a password, treat it as the
     * SAME guest identity"). If the phone belongs to a full account (LOCAL/GOOGLE), booking as a
     * guest is rejected in favor of asking the user to log in. Otherwise a brand-new guest identity
     * is created, mirroring AuthServiceImpl#registerCustomer's pattern (role lookup, unique
     * referral code, User/Customer/Wallet creation, UserRegisteredEvent).
     */
    private User resolveOrCreateGuest(GuestBookingRequest request) {
        User existing = userRepository.findByPhoneAndDeletedFalse(request.getGuestPhone()).orElse(null);
        if (existing != null) {
            if (existing.getAuthProvider() != AuthProvider.GUEST) {
                throw new ConflictException(
                        "An account with this phone number already exists. Please log in instead.");
            }
            refreshGuestDetails(existing, request);
            return existing;
        }

        if (request.getGuestEmail() != null && !request.getGuestEmail().isBlank()
                && userRepository.existsByEmailIgnoreCase(request.getGuestEmail())) {
            throw new ConflictException("An account with this email already exists. Please log in instead.");
        }

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", RoleName.CUSTOMER));

        String[] names = splitName(request.getGuestName());
        String email = request.getGuestEmail() != null && !request.getGuestEmail().isBlank()
                ? request.getGuestEmail()
                : generateUniqueGuestPlaceholderEmail(request.getGuestPhone());

        User guest = User.builder()
                .email(email)
                .phone(request.getGuestPhone())
                .password(null)
                .firstName(names[0])
                .lastName(names[1])
                .authProvider(AuthProvider.GUEST)
                .referralCode(generateUniqueReferralCode())
                .roles(Set.of(customerRole))
                .build();
        userRepository.save(guest);

        Customer customer = Customer.builder()
                .user(guest)
                .build();
        customerRepository.save(customer);

        walletService.getOrCreateWallet(guest);

        eventPublisher.publishEvent(new UserRegisteredEvent(guest));

        return guest;
    }

    private void refreshGuestDetails(User guest, GuestBookingRequest request) {
        String[] names = splitName(request.getGuestName());
        guest.setFirstName(names[0]);
        guest.setLastName(names[1]);
        // Only replace a previously auto-generated placeholder email with a real one; never
        // overwrite an email the guest deliberately provided on an earlier booking.
        if (request.getGuestEmail() != null && !request.getGuestEmail().isBlank()
                && guest.getEmail() != null && guest.getEmail().endsWith("@" + GUEST_EMAIL_DOMAIN)
                && !userRepository.existsByEmailIgnoreCase(request.getGuestEmail())) {
            guest.setEmail(request.getGuestEmail());
        }
        userRepository.save(guest);
    }

    private AppointmentResponse createBookingAsGuest(User guest, CreateBookingRequest bookingRequest) {
        Authentication previousAuthentication = SecurityContextHolder.getContext().getAuthentication();
        try {
            UserPrincipal principal = UserPrincipal.create(guest);
            UsernamePasswordAuthenticationToken guestAuthentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(guestAuthentication);
            return bookingService.createBooking(bookingRequest);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        }
    }

    private AppointmentSummaryResponse toSummaryResponse(Appointment appointment) {
        AppointmentSummaryResponse response = appointmentMapper.toSummary(appointment);
        response.setEmployeeName(appointment.getEmployee() != null ? appointment.getEmployee().getFullName() : null);
        List<String> serviceNames = appointmentServiceRepository.findAllByAppointmentId(appointment.getId()).stream()
                .map(AppointmentService::getServiceName)
                .toList();
        response.setServiceNames(serviceNames);
        return response;
    }

    private static String[] splitName(String fullName) {
        String trimmed = fullName.trim();
        int spaceIndex = trimmed.indexOf(' ');
        if (spaceIndex < 0) {
            return new String[] {trimmed, null};
        }
        return new String[] {trimmed.substring(0, spaceIndex), trimmed.substring(spaceIndex + 1).trim()};
    }

    private String generateUniqueGuestPlaceholderEmail(String phone) {
        String email = "guest+" + phone + "@" + GUEST_EMAIL_DOMAIN;
        while (userRepository.existsByEmailIgnoreCase(email)) {
            email = "guest+" + phone + "+" + RandomCodeGenerator.generateAlphanumeric(4) + "@" + GUEST_EMAIL_DOMAIN;
        }
        return email;
    }

    private String generateUniqueReferralCode() {
        String code;
        do {
            code = RandomCodeGenerator.generateAlphanumeric(AppConstants.REFERRAL_CODE_LENGTH);
        } while (userRepository.existsByReferralCode(code));
        return code;
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("No matching booking found for the given phone number and booking number");
    }
}
