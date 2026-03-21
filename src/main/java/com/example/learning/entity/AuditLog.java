package com.example.learning.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "audit_logs_user_idx", columnList = "performed_by"),
        @Index(name = "audit_logs_created_idx", columnList = "created_at")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;

    @Column(length = 1000)
    private String details;

    private String resourceType;

    private String resourceId;

    @Column(nullable = false)
    private String performedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
