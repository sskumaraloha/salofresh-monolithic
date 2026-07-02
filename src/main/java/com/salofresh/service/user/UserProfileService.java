package com.salofresh.service.user;

import com.salofresh.dto.user.UpdateProfileRequest;
import com.salofresh.dto.user.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {

    UserProfileResponse getProfile(Long userId);

    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    UserProfileResponse uploadProfileImage(Long userId, MultipartFile file);

    void deleteAccount(Long userId);

    void deactivateAccount(Long userId);
}
