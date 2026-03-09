package com.example.learning.service.impl;

import com.example.learning.dto.UserDTO;
import com.example.learning.dto.auth.*;
import com.example.learning.entity.RefreshToken;
import com.example.learning.entity.Role;
import com.example.learning.entity.User;
import com.example.learning.exception.ConflictException;
import com.example.learning.exception.ResourceNotFoundException;
import com.example.learning.exception.TokenException;
import com.example.learning.repository.RefreshTokenRepository;
import com.example.learning.repository.RoleRepository;
import com.example.learning.repository.UserRepository;
import com.example.learning.security.JwtUtil;
import com.example.learning.service.AuthService;
import com.example.learning.service.CookieService;
import com.example.learning.service.CurrentUserProvider;
import com.example.learning.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public void register(RegisterRequest request) {


        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("User with this email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);

        Role role = roleRepository.findByName("ROLE_USER");
        user.getRoles().add(role);
        log.info("User registered successfully: {}", request.getEmail());
        userRepository.save(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        log.info("User login attempt: {}", request.getEmail());

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getEmail(),
                                request.getPassword()
                        )
                );

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String accessToken = jwtUtil.generateToken(user);

        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user);

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
    public void logout(String refreshToken) {

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        Long currentUserId = currentUserProvider.getCurrentUserId();

        if (!token.getUser().getId().equals(currentUserId)) {
            throw new TokenException("Token does not belong to current user");
        }
        refreshTokenRepository.revokeByToken(refreshToken);
    }

    @Transactional
    @Override
    public void logoutAll() {

        Long userId = currentUserProvider.getCurrentUserId();

        int revoked =
                refreshTokenRepository.revokeAllByUserId(userId);

        log.info("Revoked {} tokens for user {}", revoked, userId);
    }

}