package com.example.learning.service;


import com.example.learning.entity.RefreshToken;
import com.example.learning.entity.User;

public interface RefreshTokenService {

    RefreshToken getByToken(String token);

    void verifyExpiration(RefreshToken token);

    String rotateRefreshToken(RefreshToken oldToken);

    RefreshToken createRefreshToken(User user);

    void revokeByUser(User user);
}
