package com.example.learning.service;

public interface EmailVerificationService {
    void sendVerificationEmail(String email);
    void verifyEmail(String token);
}