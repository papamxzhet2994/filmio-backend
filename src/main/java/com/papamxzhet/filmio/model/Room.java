package com.papamxzhet.filmio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank
    private String name;

    @NotBlank
    private String owner;

    @JsonIgnore
    private String password;

    private boolean isClosed;

    @Column(length = 500)
    private String avatarUrl;

    @Column(length = 500)
    private String coverUrl;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Transient
    private int participantCount;

    public Room() {
        this.createdAt = LocalDateTime.now();
    }

    public Room(String name, String owner, String password) {
        this.name = name;
        this.owner = owner;
        this.password = password;
        this.isClosed = false;
        this.createdAt = LocalDateTime.now();
    }

    @JsonProperty("hasPassword")
    public boolean isHasPassword() {
        return password != null && !password.isEmpty();
    }
}