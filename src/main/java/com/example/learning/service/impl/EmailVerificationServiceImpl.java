package com.example.learning.service.impl;

import com.example.learning.audit.Audited;
import com.example.learning.dto.EmailMessage;
import com.example.learning.entity.User;
import com.example.learning.entity.VerificationToken;
import com.example.learning.exception.ConflictException;
import com.example.learning.exception.ResourceNotFoundException;
import com.example.learning.exception.TokenException;
import com.example.learning.messaging.EmailProducer;
import com.example.learning.repository.UserRepository;
import com.example.learning.repository.VerificationTokenRepository;
import com.example.learning.service.EmailService;
import com.example.learning.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional
    public void sendVerificationEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getEnabled()) {
            throw new ConflictException("Email is already verified");
        }

        // Obrisi stari token ako postoji
        tokenRepository.deleteByUserId(user.getId());

        // Kreiraj novi token
        VerificationToken token = new VerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(Instant.now().plus(24, ChronoUnit.HOURS));
        tokenRepository.save(token);

        String verificationLink = frontendUrl + "/verify-email?token=" + token.getToken();
        emailService.sendVerificationEmail(user.getEmail(), verificationLink);

        log.info("Verification email sent to: {}", email);
    }

    @Override
    @Transactional
    @Audited(action = "EMAIL_VERIFIED", resourceType = "USER", resourceIdArgIndex = -1)
    public void verifyEmail(String token) {

        VerificationToken verificationToken = tokenRepository
                .findByToken(token)
                .orElseThrow(() -> new TokenException("Invalid or expired verification token"));

        if (verificationToken.getExpiryDate().isBefore(Instant.now())) {
            throw new TokenException("Verification token has expired");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        user.setEmailVerified(true);
        userRepository.save(user);

        tokenRepository.deleteByUserId(user.getId());

        log.info("Email verified successfully for user: {}", user.getEmail());
    }
}