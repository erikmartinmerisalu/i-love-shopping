package com.lampify.service;

import com.lampify.entity.RevokedAccessToken;
import com.lampify.repository.RevokedAccessTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TokenRevocationService {

    private final RevokedAccessTokenRepository revokedAccessTokenRepository;

    public TokenRevocationService(RevokedAccessTokenRepository revokedAccessTokenRepository) {
        this.revokedAccessTokenRepository = revokedAccessTokenRepository;
    }

    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return revokedAccessTokenRepository.findByJti(jti)
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
                .isPresent();
    }

    @Transactional
    public void revokeAccessToken(String jti, Instant expiresAt) {
        if (jti == null || jti.isBlank() || expiresAt == null) {
            return;
        }

        if (revokedAccessTokenRepository.findByJti(jti).isPresent()) {
            return;
        }

        RevokedAccessToken revoked = new RevokedAccessToken();
        revoked.setJti(jti);
        revoked.setExpiresAt(expiresAt);
        revokedAccessTokenRepository.save(revoked);
    }

    @Transactional
    public void cleanupExpiredTokens() {
        revokedAccessTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
