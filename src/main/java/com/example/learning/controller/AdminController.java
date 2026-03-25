package com.example.learning.controller;

import com.example.learning.config.ApiConstants;
import com.example.learning.dto.PagedResponse;
import com.example.learning.dto.SessionDTO;
import com.example.learning.dto.UserDTO;
import com.example.learning.dto.UserFilterRequest;
import com.example.learning.entity.AuditLog;
import com.example.learning.service.AuditLogService;
import com.example.learning.service.RefreshTokenService;
import com.example.learning.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiConstants.API_V1 + "/admin")
public class AdminController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;

    @GetMapping("/users")
    public ResponseEntity<PagedResponse<UserDTO>> getAllUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        UserFilterRequest filter = new UserFilterRequest();
        filter.setEmail(email);
        filter.setFirstName(firstName);
        filter.setEnabled(enabled);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDirection(sortDirection);

        return ResponseEntity.ok(userService.getAllFiltered(filter));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.get(id));
    }

    @PatchMapping("/users/{id}/ban")
    public ResponseEntity<Void> banUser(@PathVariable Long id) {
        userService.setBanStatus(id, true);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/unban")
    public ResponseEntity<Void> unbanUser(@PathVariable Long id) {
        userService.setBanStatus(id, false);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/promote")
    public ResponseEntity<Void> promoteToAdmin(@PathVariable Long id) {
        userService.promoteToAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{id}/sessions")
    public ResponseEntity<List<SessionDTO>> getUserSessions(@PathVariable Long id) {
        return ResponseEntity.ok(refreshTokenService.getActiveSessionsByUserId(id));
    }

    @DeleteMapping("/users/{id}/sessions")
    public ResponseEntity<Void> revokeAllUserSessions(@PathVariable Long id) {
        refreshTokenService.revokeAllSessionsByUserId(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOnly() {

        log.debug("Admin endpoint /api/auth/login called");
        return "Admin access granted successfully";
    }

    @PatchMapping("/users/{id}/demote")
    public ResponseEntity<Void> demoteFromAdmin(@PathVariable Long id) {
        userService.demoteFromAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogService.getAllLogs());
    }

    @GetMapping("/audit-logs/user/{email}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByUser(@PathVariable String email) {
        return ResponseEntity.ok(auditLogService.getLogsByUser(email));
    }


}
 