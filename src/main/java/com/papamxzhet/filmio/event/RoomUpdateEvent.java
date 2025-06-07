package com.papamxzhet.filmio.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

@Getter
public class RoomUpdateEvent extends ApplicationEvent {
    private final UUID roomId;

    public RoomUpdateEvent(Object source, UUID roomId) {
        super(source);
        this.roomId = roomId;
    }

}