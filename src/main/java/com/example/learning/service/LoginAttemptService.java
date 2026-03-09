package com.example.learning.service;

public interface LoginAttemptService {
    void loginFailed(String email);
    void loginSucceeded(String email);
    boolean isLocked(String email);
}