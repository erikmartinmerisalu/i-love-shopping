package com.lampify.repository;

import com.lampify.entity.RevokedAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, Long> {
    Optional<RevokedAccessToken> findByJti(String jti);
    void deleteByExpiresAtBefore(Instant instant);
}
