package com.example.learning.service.impl;

import com.example.learning.audit.Audited;
import com.example.learning.dto.EmailMessage;
import com.example.learning.dto.auth.ForgotPasswordRequest;
import com.example.learning.dto.auth.ResetPasswordRequest;
import com.example.learning.entity.PasswordResetToken;
import com.example.learning.entity.User;
import com.example.learning.exception.ResourceNotFoundException;
import com.example.learning.exception.TokenException;
import com.example.learning.messaging.EmailProducer;
import com.example.learning.repository.PasswordResetTokenRepository;
import com.example.learning.repository.UserRepository;
import com.example.learning.service.EmailService;
import com.example.learning.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailProducer emailProducer;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.reset-token.expiration-minutes}")
    private int expirationMinutes;

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {

        // Namerno ne bacamo grešku ako user ne postoji
        // jer ne želimo da otkrijemo da li email postoji u sistemu
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {

            // Obrišemo stare tokene za ovog usera
            tokenRepository.deleteAllByUserId(user.getId());

            // Kreiramo novi token
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setUser(user);
            resetToken.setExpiryDate(
                    Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES)
            );
            resetToken.setUsed(false);
            tokenRepository.save(resetToken);

            // Šaljemo email
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken.getToken();
            emailProducer.sendEmailMessage(
                    new EmailMessage(user.getEmail(), "PASSWORD_RESET", resetLink)
            );

            log.info("Password reset token created for user: {}", user.getEmail());
        });
    }

    @Override
    @Transactional
    @Audited(action = "PASSWORD_RESET", resourceType = "USER")
    public void resetPassword(ResetPasswordRequest request) {

        PasswordResetToken resetToken = tokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> new TokenException("Invalid or expired reset token"));

        if (resetToken.isUsed()) {
            throw new TokenException("Reset token has already been used");
        }

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new TokenException("Reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password reset successful for user: {}", user.getEmail());
    }
}