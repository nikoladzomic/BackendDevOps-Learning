package com.example.learning.service.impl;

import com.example.learning.entity.RefreshToken;
import com.example.learning.entity.User;
import com.example.learning.exception.ResourceNotFoundException;
import com.example.learning.exception.TokenException;
import com.example.learning.repository.RefreshTokenRepository;
import com.example.learning.repository.UserRepository;
import com.example.learning.service.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock UserRepository userRepository;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private User testUser;
    private RefreshToken validToken;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");

        validToken = new RefreshToken();
        validToken.setId(1L);
        validToken.setToken("valid-refresh-token");
        validToken.setUser(testUser);
        validToken.setExpiryDate(Instant.now().plusSeconds(3600));
        validToken.setRevoked(false);
    }

    @Test
    void verifyExpiration_withValidToken_shouldNotThrow() {
        assertThatCode(() -> refreshTokenService.verifyExpiration(validToken))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyExpiration_withExpiredToken_shouldThrowTokenException() {
        validToken.setExpiryDate(Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(validToken))
                .isInstanceOf(TokenException.class);
    }

    @Test
    void verifyExpiration_withRevokedToken_shouldThrowTokenException() {
        validToken.setRevoked(true);

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(validToken))
                .isInstanceOf(TokenException.class);
    }

    @Test
    void rotateRefreshToken_shouldUpdateTokenAndExpiry() {
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String newToken = refreshTokenService.rotateRefreshToken(validToken);

        assertThat(newToken).isNotEqualTo("valid-refresh-token");
        assertThat(validToken.getExpiryDate()).isAfter(Instant.now());
        assertThat(validToken.isRevoked()).isFalse();
        verify(refreshTokenRepository).save(validToken);
    }

    @Test
    void revokeSession_withTokenBelongingToOtherUser_shouldThrowTokenException() {
        User otherUser = new User();
        otherUser.setId(99L);
        validToken.setUser(otherUser);

        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(validToken));

        assertThatThrownBy(() -> refreshTokenService.revokeSession(1L))
                .isInstanceOf(TokenException.class);
    }

    @Test
    void revokeSession_withValidOwner_shouldSetRevokedTrue() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(validToken));
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        refreshTokenService.revokeSession(1L);

        assertThat(validToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(validToken);
    }

    @Test
    void getByToken_whenNotFound_shouldThrowTokenException() {
        when(refreshTokenRepository.findByToken("nonexistent"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.getByToken("nonexistent"))
                .isInstanceOf(TokenException.class);
    }
}