package com.example.learning.service;

public interface TokenBlacklistService {
    void blacklistToken(String token, long expirationMs);
    boolean isBlacklisted(String token);
}