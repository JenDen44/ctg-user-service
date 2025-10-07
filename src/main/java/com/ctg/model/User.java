package com.ctg.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class User {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name="token_version", nullable=false)
    private int tokenVersion;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @CreationTimestamp
    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;
}
