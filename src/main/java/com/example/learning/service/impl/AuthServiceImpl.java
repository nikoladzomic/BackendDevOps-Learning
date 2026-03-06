package com.example.learning.service.impl;

import com.example.learning.dto.UserDTO;
import com.example.learning.dto.auth.*;
import com.example.learning.entity.RefreshToken;
import com.example.learning.entity.Role;
import com.example.learning.entity.User;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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


    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
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
            throw new RuntimeException("User already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);

        Role role = roleRepository.findByName("ROLE_USER");
        user.getRoles().add(role);
        logger.info("User registered successfully: {}", request.getEmail());
        userRepository.save(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        logger.info("User login attempt: {}", request.getEmail());

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getEmail(),
                                request.getPassword()
                        )
                );

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtUtil.generateToken(user);

        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user);

        logger.info("Login successful for email: {}", request.getEmail());

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
    public void logout(String refreshToken) {
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

    @Transactional(readOnly = true)
    public UserDTO getCurrentUser() {
        Long userId = currentUserProvider.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setRoles(user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet()));
        dto.setEnabled(user.getEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }

}