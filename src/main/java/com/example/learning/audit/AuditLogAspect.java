package com.example.learning.audit;

import com.example.learning.entity.AuditLog;
import com.example.learning.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;

    @Around("@annotation(audited)")
    public Object logAudit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {

        // Ko poziva
        String performedBy = getCurrentUserEmail();

        // Koji je prvi argument — obicno je to ID resursa
        String resourceId = extractResourceId(joinPoint.getArgs());

        Object result;
        try {
            // Izvrsi originalnu metodu
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            log.warn("Audited method {} failed: {}", audited.action(), ex.getMessage());
            throw ex;
        }

        // Sacuvaj audit log samo ako je metoda uspesno izvrsena
        AuditLog auditLog = new AuditLog();
        auditLog.setPerformedBy(performedBy);
        auditLog.setAction(audited.action());
        auditLog.setResourceId(resourceId);
        auditLog.setResourceType(audited.resourceType());
        auditLog.setCreatedAt(Instant.now());
        auditLogRepository.save(auditLog);

        log.info("Audit: {} performed {} on {} {}",
                performedBy, audited.action(), audited.resourceType(), resourceId);

        return result;
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "SYSTEM";
    }

    private String extractResourceId(Object[] args) {
        if (args != null && args.length > 0 && args[0] != null) {
            return args[0].toString();
        }
        return null;
    }
}