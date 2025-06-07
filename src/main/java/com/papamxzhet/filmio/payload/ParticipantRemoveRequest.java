package com.papamxzhet.filmio.payload;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ParticipantRemoveRequest {
    private String username;

    public ParticipantRemoveRequest() {}

    public ParticipantRemoveRequest(String username) {
        this.username = username;
    }
}