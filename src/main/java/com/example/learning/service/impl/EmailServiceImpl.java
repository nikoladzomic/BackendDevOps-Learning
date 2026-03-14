package com.example.learning.service.impl;

import com.example.learning.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@learningapp.com");
        message.setTo(to);
        message.setSubject("Password Reset Request");
        message.setText("""
                You requested a password reset.
                
                Click the link below to reset your password:
                %s
                
                This link will expire in 15 minutes.
                
                If you did not request a password reset, please ignore this email.
                """.formatted(resetLink));

        mailSender.send(message);
        log.info("Password reset email sent to: {}", to);
    }

    @Override
    public void sendVerificationEmail(String to, String verificationLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@learningapp.com");
        message.setTo(to);
        message.setSubject("Please verify your email");
        message.setText("""
            Welcome! Please verify your email address.
            
            Click the link below to verify your account:
            %s
            
            This link will expire in 24 hours.
            
            If you did not create an account, please ignore this email.
            """.formatted(verificationLink));

        mailSender.send(message);
        log.info("Verification email sent to: {}", to);
    }
}