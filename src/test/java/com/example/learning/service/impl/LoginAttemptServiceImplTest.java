package com.example.learning.service.impl;

import com.example.learning.entity.User;
import com.example.learning.exception.ResourceNotFoundException;
import com.example.learning.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LoginAttemptServiceImpl loginAttemptService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");
        testUser.setFailedLoginAttempts(0);
        testUser.setLockedUntil(null);
    }

    // ─── loginFailed ─────────────────────────────────────────────────────────

    @Test
    void loginFailed_shouldIncrementFailedAttempts() {
        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(testUser));

        loginAttemptService.loginFailed("test@test.com");

        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(1);
        verify(userRepository).save(testUser);
    }

    @Test
    void loginFailed_afterMaxAttempts_shouldSetLockedUntil() {
        testUser.setFailedLoginAttempts(4); // jedan pre max-a

        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(testUser));

        loginAttemptService.loginFailed("test@test.com");

        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(testUser.getLockedUntil()).isNotNull();
        assertThat(testUser.getLockedUntil()).isAfter(Instant.now());
        verify(userRepository).save(testUser);
    }

    @Test
    void loginFailed_withNonExistentUser_shouldThrowResourceNotFoundException() {
        when(userRepository.findByEmail("ghost@test.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginAttemptService.loginFailed("ghost@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── loginSucceeded ───────────────────────────────────────────────────────

    @Test
    void loginSucceeded_shouldResetAttemptsAndClearLock() {
        testUser.setFailedLoginAttempts(3);
        testUser.setLockedUntil(Instant.now().plusSeconds(600));

        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(testUser));

        loginAttemptService.loginSucceeded("test@test.com");

        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(testUser.getLockedUntil()).isNull();
        verify(userRepository).save(testUser);
    }

    // ─── isLocked ─────────────────────────────────────────────────────────────

    @Test
    void isLocked_withNoLock_shouldReturnFalse() {
        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(testUser));

        assertThat(loginAttemptService.isLocked("test@test.com")).isFalse();
    }

    @Test
    void isLocked_withActiveLock_shouldReturnTrue() {
        testUser.setLockedUntil(Instant.now().plusSeconds(600));

        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(testUser));

        assertThat(loginAttemptService.isLocked("test@test.com")).isTrue();
    }

    @Test
    void isLocked_withExpiredLock_shouldReturnFalse() {
        testUser.setLockedUntil(Instant.now().minusSeconds(1));

        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(testUser));

        assertThat(loginAttemptService.isLocked("test@test.com")).isFalse();
    }
}