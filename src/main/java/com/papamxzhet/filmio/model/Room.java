package com.papamxzhet.filmio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
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

    private String avatarUrl;

    @Transient
    private int participantCount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_participants", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "participant")
    private Set<String> participants = new HashSet<>();

    public Room() {}

    public Room(String name, String owner, String password) {
        this.name = name;
        this.owner = owner;
        this.password = password;
        this.isClosed = false;
    }

    // Геттер для поля hasPassword, которое зависит от password
    @JsonProperty("hasPassword")
    public boolean isHasPassword() {
        return password != null && !password.isEmpty();
    }
}
