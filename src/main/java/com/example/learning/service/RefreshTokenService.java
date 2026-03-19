package com.example.learning.service;


import com.example.learning.dto.SessionDTO;
import com.example.learning.entity.RefreshToken;
import com.example.learning.entity.User;

import java.util.List;

public interface RefreshTokenService {

    RefreshToken getByToken(String token);

    void verifyExpiration(RefreshToken token);

    String rotateRefreshToken(RefreshToken oldToken);

    RefreshToken createRefreshToken(User user);

    void revokeByUser(User user);

    void revokeSession(Long sessionId);

    List<SessionDTO> getActiveSessionsForCurrentUser();

    List<SessionDTO> getActiveSessionsByUserId(Long userId);

    void revokeAllSessionsByUserId(Long userId);
}
