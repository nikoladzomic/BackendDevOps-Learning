package com.example.learning.service;

import com.example.learning.entity.AuditLog;
import java.util.List;

public interface AuditLogService {
    List<AuditLog> getAllLogs();
    List<AuditLog> getLogsByUser(String email);
    List<AuditLog> getLogsByResource(String resourceId, String resourceType);
}