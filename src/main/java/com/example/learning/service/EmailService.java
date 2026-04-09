package com.example.learning.service;

public interface EmailService {
    void sendPasswordResetEmail(String to, String resetLink);
    void sendVerificationEmail(String to, String verificationLink);
    // EmailService.java — dodaj:
    void sendOrderConfirmationEmail(String to, String orderLink);
    void sendOrderShippedEmail(String to, String orderLink);
}