package com.example.learning.service;

import com.example.learning.dto.auth.ForgotPasswordRequest;
import com.example.learning.dto.auth.ResetPasswordRequest;

public interface PasswordResetService {
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}