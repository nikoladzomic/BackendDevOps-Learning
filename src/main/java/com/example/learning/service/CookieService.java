package com.example.learning.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CookieService {

    private final boolean secureCookie;

    public CookieService(@Value("${app.cookie.secure}") boolean secureCookie) {
        this.secureCookie = secureCookie;
    }

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/auth/")
                .sameSite("Strict")
                .maxAge(Duration.ofDays(7))
                .build();
    }

    public ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/auth/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }

}
