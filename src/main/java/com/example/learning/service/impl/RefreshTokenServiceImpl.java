package com.example.learning.service.impl;

import com.example.learning.entity.RefreshToken;
import com.example.learning.entity.User;
import com.example.learning.exception.TokenException;
import com.example.learning.repository.RefreshTokenRepository;
import com.example.learning.service.RefreshTokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public RefreshToken getByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenException("Refresh token not found"));
    }

    @Override
    public void verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now()) || token.isRevoked()) {
            throw new TokenException("Refresh token is expired or revoked");
        }
    }

    @Override
    @Transactional
    public String rotateRefreshToken(RefreshToken oldToken) {
        verifyExpiration(oldToken);

        oldToken.setToken(UUID.randomUUID().toString());
        oldToken.setExpiryDate(Instant.now().plus(7, ChronoUnit.DAYS));
        oldToken.setRevoked(false);

        refreshTokenRepository.save(oldToken); // UPDATE
        return oldToken.getToken();
    }

    @Override
    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(Instant.now().plus(7, ChronoUnit.DAYS));
        token.setRevoked(false);

        return refreshTokenRepository.save(token);
    }

    @Override
    public void revokeByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}

