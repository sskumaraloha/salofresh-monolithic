package com.salofresh.repository;

import com.salofresh.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    List<DeviceToken> findAllByUserIdAndActiveTrue(Long userId);

    Optional<DeviceToken> findByToken(String token);

    void deleteByToken(String token);
}
