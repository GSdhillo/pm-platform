package com.gurjeet.pm.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class UserEntity {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(name = "display_name", nullable = false)
    private String displayName;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UserEntity(String email, String displayName, String passwordHash) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
    }
}
