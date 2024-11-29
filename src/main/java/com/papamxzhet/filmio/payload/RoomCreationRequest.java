package com.papamxzhet.filmio.payload;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RoomCreationRequest {
    private String name;
    private String password;
    private boolean isClosed;
}