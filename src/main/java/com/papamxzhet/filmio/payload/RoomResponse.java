package com.papamxzhet.filmio.payload;


import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
@Setter
@Getter
public class RoomResponse {
    private UUID id;
    private String name;
    private String owner;
    private boolean hasPassword;
    private boolean isClosed;
    private int participantCount;

    public RoomResponse(UUID id, String name, String owner, boolean hasPassword, boolean isClosed, int participantCount) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.hasPassword = hasPassword;
        this.isClosed = isClosed;
        this.participantCount = participantCount;
    }
}
