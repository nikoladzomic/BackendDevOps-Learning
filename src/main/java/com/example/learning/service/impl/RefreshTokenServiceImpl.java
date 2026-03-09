package com.example.learning.service.impl;

import com.example.learning.dto.SessionDTO;
import com.example.learning.entity.RefreshToken;
import com.example.learning.entity.User;
import com.example.learning.exception.ResourceNotFoundException;
import com.example.learning.exception.TokenException;
import com.example.learning.repository.RefreshTokenRepository;
import com.example.learning.service.CurrentUserProvider;
import com.example.learning.service.RefreshTokenService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final CurrentUserProvider currentUserProvider;

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

    @Override
    @Transactional(readOnly = true)
    public List<SessionDTO> getActiveSessionsForCurrentUser() {
        Long userId = currentUserProvider.getCurrentUserId();

        return refreshTokenRepository
                .findActiveSessionsByUserId(userId, Instant.now())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void revokeSession(Long sessionId) {
        Long userId = currentUserProvider.getCurrentUserId();

        RefreshToken token = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (!token.getUser().getId().equals(userId)) {
            throw new TokenException("Session does not belong to current user");
        }

        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    private SessionDTO mapToDTO(RefreshToken token) {
        SessionDTO dto = new SessionDTO();
        dto.setId(token.getId());
        dto.setToken(token.getToken());
        dto.setExpiryDate(token.getExpiryDate());
        dto.setRevoked(token.isRevoked());
        return dto;
    }
}

