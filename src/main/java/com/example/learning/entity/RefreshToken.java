package com.example.learning.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens",
            indexes = {
                    @Index( name = "refresh_tokens_idx", columnList = "token" )
            })
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Instant expiryDate;
    private boolean revoked;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "device_info", length = 500)
    private String deviceInfo;
}
