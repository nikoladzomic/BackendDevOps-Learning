package com.example.learning.messaging;

import com.example.learning.config.RabbitMQConfig;
import com.example.learning.dto.EmailMessage;
import com.example.learning.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void processEmailMessage(EmailMessage message) {
        log.info("Processing email message: type={}, to={}",
                message.getType(), message.getTo());

        try {
            switch (message.getType()) {
                case "VERIFICATION" ->
                        emailService.sendVerificationEmail(message.getTo(), message.getLink());
                case "PASSWORD_RESET" ->
                        emailService.sendPasswordResetEmail(message.getTo(), message.getLink());
                default ->
                        log.warn("Unknown email type: {}", message.getType());
            }
        } catch (Exception ex) {
            log.error("Failed to process email message: {}", ex.getMessage());
            throw ex; // RabbitMQ će poslati u DLQ
        }
    }
}