package com.papamxzhet.filmio.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomResponse {
    private UUID id;
    private String name;
    private String owner;
    private boolean hasPassword;
    private boolean closed;
    private int participantCount;
    private String avatarUrl;
    private String coverUrl;
    private String description;
    private LocalDateTime createdAt;

    public RoomResponse(UUID id, String name, String owner, boolean hasPassword,
                        boolean closed, int participantCount, String avatarUrl,
                        String description, LocalDateTime createdAt, String coverUrl) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.hasPassword = hasPassword;
        this.closed = closed;
        this.participantCount = participantCount;
        this.avatarUrl = avatarUrl;
        this.description = description;
        this.createdAt = createdAt;
        this.coverUrl = coverUrl;
    }
}