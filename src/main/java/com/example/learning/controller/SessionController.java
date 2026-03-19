package com.example.learning.controller;

import com.example.learning.config.ApiConstants;
import com.example.learning.dto.SessionDTO;
import com.example.learning.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiConstants.API_V1 + "/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final RefreshTokenService refreshTokenService;

    @GetMapping
    public ResponseEntity<List<SessionDTO>> getActiveSessions() {
        return ResponseEntity.ok(refreshTokenService.getActiveSessionsForCurrentUser());
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> revokeSession(@PathVariable Long sessionId) {
        refreshTokenService.revokeSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}