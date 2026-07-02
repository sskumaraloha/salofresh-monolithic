package com.salofresh.repository;

import com.salofresh.entity.RefreshToken;
import com.salofresh.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    List<RefreshToken> findAllByUserAndRevokedFalse(User user);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.user = :user")
    void revokeAllByUser(@Param("user") User user);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.token = :token")
    void revokeByToken(@Param("token") String token);

    @Modifying
    @Query("delete from RefreshToken r where r.expiryDate < :now")
    void deleteAllExpired(@Param("now") Instant now);
}
