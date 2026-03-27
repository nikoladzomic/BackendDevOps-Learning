package com.example.learning.service;


import com.example.learning.dto.auth.*;

public interface AuthService {

    void register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(String refreshToken);
    void logout(String refreshToken, String accessToken);
    void logoutAll();
}
