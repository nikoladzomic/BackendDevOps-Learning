package com.example.learning.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class SessionDTO {
    private Long id;
    private String token;
    private Instant expiryDate;
    private boolean revoked;
    private Instant createdAt;
}