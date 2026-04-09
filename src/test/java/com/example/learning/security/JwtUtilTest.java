package com.example.learning.security;

import com.example.learning.entity.Role;
import com.example.learning.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Isti format kao u application.properties
        jwtUtil = new JwtUtil(
                "test-secret-key-minimum-32-characters-long!!",
                86400000L // 24h
        );

        Role role = new Role("ROLE_USER");
        testUser = new User();
        testUser.setEmail("test@test.com");
        testUser.setPassword("encoded");
        testUser.setEnabled(true);
        testUser.setRoles(Set.of(role));
    }

    @Test
    void generateToken_shouldReturnNonNullToken() {
        String token = jwtUtil.generateToken(testUser);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void extractEmail_shouldReturnCorrectEmail() {
        String token = jwtUtil.generateToken(testUser);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("test@test.com");
    }

    @Test
    void isTokenValid_withCorrectUser_shouldReturnTrue() {
        String token = jwtUtil.generateToken(testUser);
        UserDetails userDetails = new CustomUserDetails(testUser);
        assertThat(jwtUtil.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_withWrongEmail_shouldReturnFalse() {
        String token = jwtUtil.generateToken(testUser);

        User otherUser = new User();
        otherUser.setEmail("other@test.com");
        otherUser.setPassword("encoded");
        otherUser.setEnabled(true);
        otherUser.setRoles(Set.of());

        UserDetails otherDetails = new CustomUserDetails(otherUser);
        assertThat(jwtUtil.isTokenValid(token, otherDetails)).isFalse();
    }

    @Test
    void isTokenValid_withExpiredToken_shouldReturnFalse() {
        // Token koji je istekao pre 1ms
        JwtUtil expiredJwtUtil = new JwtUtil(
                "test-secret-key-minimum-32-characters-long!!",
                -1L
        );
        String token = expiredJwtUtil.generateToken(testUser);
        UserDetails userDetails = new CustomUserDetails(testUser);
        assertThat(jwtUtil.isTokenValid(token, userDetails)).isFalse();
    }

    @Test
    void getRemainingExpiration_shouldBePositiveForFreshToken() {
        String token = jwtUtil.generateToken(testUser);
        assertThat(jwtUtil.getRemainingExpiration(token)).isPositive();
    }

    @Test
    void getRemainingExpiration_shouldBeNegativeForExpiredToken() {
        JwtUtil expiredJwtUtil = new JwtUtil(
                "test-secret-key-minimum-32-characters-long!!",
                -1L
        );
        String token = expiredJwtUtil.generateToken(testUser);
        assertThat(jwtUtil.getRemainingExpiration(token)).isNegative();
    }
}