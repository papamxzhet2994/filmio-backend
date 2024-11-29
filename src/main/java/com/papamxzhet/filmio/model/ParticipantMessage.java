package com.papamxzhet.filmio.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class ParticipantMessage {

    private String username;
    private UUID roomId;

    public ParticipantMessage() {}

    public ParticipantMessage(String username, UUID roomId) {
        this.username = username;
        this.roomId = roomId;
    }

}
