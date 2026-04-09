package com.example.learning.service.impl;

import com.example.learning.dto.auth.*;
import com.example.learning.entity.*;
import com.example.learning.exception.*;
import com.example.learning.repository.*;
import com.example.learning.security.JwtUtil;
import com.example.learning.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock RefreshTokenService refreshTokenService;
    @Mock CookieService cookieService;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock LoginAttemptService loginAttemptService;
    @Mock EmailVerificationService emailVerificationService;
    @Mock TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = new Role("ROLE_USER");

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");
        testUser.setPassword("encodedPassword");
        testUser.setEnabled(true);
        testUser.setEmailVerified(true);
        testUser.setRoles(Set.of(userRole));
    }

    // ─── REGISTER ────────────────────────────────────────────────────────────

    @Test
    void register_withNewEmail_shouldSaveUserAndSendVerification() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@test.com");
        request.setPassword("password123");
        request.setFirstName("Nikola");
        request.setLastName("Test");

        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(userRole);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");

        authService.register(request);

        verify(userRepository).save(any(User.class));
        verify(emailVerificationService).sendVerificationEmail("new@test.com");
    }

    @Test
    void register_withExistingEmail_shouldThrowConflictException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@test.com");
        request.setPassword("password123");
        request.setFirstName("Nikola");
        request.setLastName("Test");

        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
    }

    // ─── LOGIN ────────────────────────────────────────────────────────────────

    @Test
    void login_withValidCredentials_shouldReturnTokens() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("password123");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token-value");
        refreshToken.setExpiryDate(Instant.now().plusSeconds(3600));

        when(loginAttemptService.isLocked("test@test.com")).thenReturn(false);
        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(testUser)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(testUser)).thenReturn(refreshToken);

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-value");
    }

    @Test
    void login_withLockedAccount_shouldThrowAccountLockedException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("wrong");

        when(loginAttemptService.isLocked("test@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountLockedException.class);

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_withBadCredentials_shouldIncrementFailedAttemptsAndRethrow() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("wrongpassword");

        when(loginAttemptService.isLocked("test@test.com")).thenReturn(false);
        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(testUser));
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginAttemptService).loginFailed("test@test.com");
        verify(loginAttemptService, never()).loginSucceeded(any());
    }

    @Test
    void login_withUnverifiedAccount_shouldThrowAccountNotVerifiedException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("password123");

        testUser.setEnabled(false);
        testUser.setEmailVerified(false);

        when(loginAttemptService.isLocked("test@test.com")).thenReturn(false);
        when(userRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountNotVerifiedException.class);
    }

    // ─── LOGOUT ───────────────────────────────────────────────────────────────

    @Test
    void logout_withValidToken_shouldRevokeAndBlacklist() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        refreshToken.setUser(testUser);

        when(refreshTokenRepository.findByToken("refresh-token"))
                .thenReturn(Optional.of(refreshToken));
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(jwtUtil.getRemainingExpiration("access-token")).thenReturn(60000L);

        authService.logout("refresh-token", "access-token");

        verify(refreshTokenRepository).revokeByToken("refresh-token");
        verify(tokenBlacklistService).blacklistToken("access-token", 60000L);
    }

    @Test
    void logout_withTokenBelongingToOtherUser_shouldThrowTokenException() {
        User otherUser = new User();
        otherUser.setId(99L);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        refreshToken.setUser(otherUser);

        when(refreshTokenRepository.findByToken("refresh-token"))
                .thenReturn(Optional.of(refreshToken));
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);

        assertThatThrownBy(() -> authService.logout("refresh-token", "access-token"))
                .isInstanceOf(TokenException.class);

        verify(refreshTokenRepository, never()).revokeByToken(any());
    }
}