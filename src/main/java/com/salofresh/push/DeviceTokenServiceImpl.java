package com.salofresh.push;

import com.salofresh.common.enums.DevicePlatform;
import com.salofresh.entity.DeviceToken;
import com.salofresh.entity.User;
import com.salofresh.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceTokenServiceImpl implements DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    @Transactional
    public void registerToken(User user, String token, DevicePlatform platform) {
        DeviceToken deviceToken = deviceTokenRepository.findByToken(token)
                .orElseGet(() -> DeviceToken.builder().token(token).build());
        deviceToken.setUser(user);
        deviceToken.setPlatform(platform);
        deviceToken.setActive(true);
        deviceTokenRepository.save(deviceToken);
    }

    @Override
    @Transactional
    public void unregisterToken(String token) {
        deviceTokenRepository.findByToken(token).ifPresent(deviceToken -> {
            deviceToken.setActive(false);
            deviceTokenRepository.save(deviceToken);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceToken> listActiveTokensForUser(Long userId) {
        return deviceTokenRepository.findAllByUserIdAndActiveTrue(userId);
    }
}
