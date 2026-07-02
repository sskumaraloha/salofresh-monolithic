package com.salofresh.repository;

import com.salofresh.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    boolean existsByTokenId(String tokenId);

    @Modifying
    @Query("delete from BlacklistedToken b where b.expiryDate < :now")
    void deleteAllExpired(@Param("now") Instant now);
}
