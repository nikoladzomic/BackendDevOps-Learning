package com.example.learning.repository;


import com.example.learning.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE a.performedBy = :email ORDER BY a.createdAt DESC")
    List<AuditLog> findByPerformed(@Param("email") String email);

    @Query("SELECT a FROM AuditLog a WHERE a.resourceId = :resourceId AND a.resourceType = :resourceType ORDER BY a.createdAt DESC")
    List<AuditLog> findByResourceType(
            @Param("resourceId") String resourceId,
            @Param("resourceType") String resourceType
    );
}
