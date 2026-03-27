package com.example.learning.service.impl;

import com.example.learning.audit.Audited;
import com.example.learning.dto.UserDTO;
import com.example.learning.dto.auth.*;
import com.example.learning.entity.RefreshToken;
import com.example.learning.entity.Role;
import com.example.learning.entity.User;
import com.example.learning.exception.*;
import com.example.learning.repository.RefreshTokenRepository;
import com.example.learning.repository.RoleRepository;
import com.example.learning.repository.UserRepository;
import com.example.learning.security.JwtUtil;
import com.example.learning.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final CookieService cookieService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CurrentUserProvider currentUserProvider;
    private final LoginAttemptService loginAttemptService;
    private final EmailVerificationService emailVerificationService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    @Transactional
    public void register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("User with this email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(false); // Ne može da se uloguje dok ne verifikuje email
        user.setCreatedAt(new Date());
        user.setEmailVerified(false);

        Role role = roleRepository.findByName("ROLE_USER");
        user.getRoles().add(role);
        userRepository.save(user);

        // Pošalji verifikacioni email
        emailVerificationService.sendVerificationEmail(user.getEmail());

        log.info("User registered successfully: {}", request.getEmail());
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        // Prvo proveri lock
        if (loginAttemptService.isLocked(request.getEmail())) {
            throw new AccountLockedException(
                    "Account is locked due to multiple failed attempts. Try again in 15 minutes."
            );
        }

        // Nadji usera pre autentikacije da proverimo status
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Proveri da li je verifikovan
        if (!user.getEnabled() && !isBanned(user)) {
            throw new AccountNotVerifiedException(
                    "Please verify your email before logging in. Check your inbox."
            );
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            loginAttemptService.loginFailed(request.getEmail());
            throw ex;
        }

        loginAttemptService.loginSucceeded(request.getEmail());

        String accessToken = jwtUtil.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("Login successful for email: {}", request.getEmail());

        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    @Override
    public AuthResponse refreshToken(String refreshTokenValue) {

        RefreshToken refreshToken =
                refreshTokenService.getByToken(refreshTokenValue);

        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();

        String newAccessToken = jwtUtil.generateToken(user);
        String newRefreshToken =
                refreshTokenService.rotateRefreshToken(refreshToken);

        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(String refreshToken, String accessToken) {

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new TokenException("Refresh token not found"));

        Long currentUserId = currentUserProvider.getCurrentUserId();

        if (!token.getUser().getId().equals(currentUserId)) {
            throw new TokenException("Token does not belong to current user");
        }

        // Revokuj refresh token u bazi
        refreshTokenRepository.revokeByToken(refreshToken);

        // Blacklistuj access token u Redisu
        if (accessToken != null) {
            long remainingTtl = jwtUtil.getRemainingExpiration(accessToken);
            if (remainingTtl > 0) {
                tokenBlacklistService.blacklistToken(accessToken, remainingTtl);
            }
        }
    }

    @Transactional
    @Override
    public void logoutAll() {

        Long userId = currentUserProvider.getCurrentUserId();

        int revoked =
                refreshTokenRepository.revokeAllByUserId(userId);

        log.info("Revoked {} tokens for user {}", revoked, userId);
    }

    private boolean isBanned(User user) {
        return user.isEmailVerified() && !user.getEnabled();
    }
}