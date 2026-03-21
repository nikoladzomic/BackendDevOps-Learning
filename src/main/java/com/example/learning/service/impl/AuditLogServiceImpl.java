package com.example.learning.service.impl;

import com.example.learning.entity.AuditLog;
import com.example.learning.repository.AuditLogRepository;
import com.example.learning.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByUser(String email) {
        return auditLogRepository
                .findByPerformed(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByResource(String resourceId, String resourceType) {
        return auditLogRepository
                .findByResourceType(
                        resourceId,
                        resourceType
                );
    }
}