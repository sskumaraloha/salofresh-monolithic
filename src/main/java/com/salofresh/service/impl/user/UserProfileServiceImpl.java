package com.salofresh.service.impl.user;

import com.salofresh.common.enums.AccountStatus;
import com.salofresh.dto.user.UpdateProfileRequest;
import com.salofresh.dto.user.UserProfileResponse;
import com.salofresh.entity.Customer;
import com.salofresh.entity.User;
import com.salofresh.exception.ConflictException;
import com.salofresh.exception.FileStorageException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.file.FileStorageService;
import com.salofresh.repository.CustomerRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.service.user.UserProfileService;
import com.salofresh.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final String PROFILE_IMAGE_SUBDIRECTORY = "profile-images";

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final WalletService walletService;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = getActiveUser(userId);
        return buildProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getActiveUser(userId);

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setGender(request.getGender());
        user.setDateOfBirth(request.getDateOfBirth());

        String newPhone = request.getPhone();
        if (newPhone != null && !newPhone.equals(user.getPhone())) {
            if (userRepository.existsByPhone(newPhone)) {
                throw new ConflictException("Phone number is already in use");
            }
            user.setPhone(newPhone);
            user.setPhoneVerified(false);
        }

        userRepository.save(user);
        return buildProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse uploadProfileImage(Long userId, MultipartFile file) {
        User user = getActiveUser(userId);
        String previousImageUrl = user.getProfileImageUrl();

        String newImageUrl = fileStorageService.store(file, PROFILE_IMAGE_SUBDIRECTORY);
        user.setProfileImageUrl(newImageUrl);
        userRepository.save(user);

        if (previousImageUrl != null && !previousImageUrl.isBlank()) {
            try {
                fileStorageService.delete(previousImageUrl);
            } catch (FileStorageException ex) {
                log.warn("Failed to delete previous profile image [{}] for user {}", previousImageUrl, userId, ex);
            }
        }

        return buildProfileResponse(user);
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId) {
        User user = findUser(userId);
        user.setDeleted(true);
        user.setAccountStatus(AccountStatus.DELETED);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateAccount(Long userId) {
        User user = getActiveUser(userId);
        user.setAccountStatus(AccountStatus.INACTIVE);
        userRepository.save(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private User getActiveUser(Long userId) {
        User user = findUser(userId);
        if (user.isDeleted()) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        return user;
    }

    private UserProfileResponse buildProfileResponse(User user) {
        Customer customer = customerRepository.findByUserId(user.getId()).orElse(null);
        BigDecimal walletBalance = walletService.getBalance(user.getId());

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profileImageUrl(user.getProfileImageUrl())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .referralCode(user.getReferralCode())
                .membershipLevel(customer != null ? customer.getMembershipLevel() : null)
                .totalBookings(customer != null ? customer.getTotalBookings() : 0)
                .totalSpent(customer != null ? customer.getTotalSpent() : BigDecimal.ZERO)
                .walletBalance(walletBalance)
                .build();
    }
}
