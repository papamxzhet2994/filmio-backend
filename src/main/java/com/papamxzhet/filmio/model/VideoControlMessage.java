package com.papamxzhet.filmio.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VideoControlMessage {
    private String roomId;
    private String videoUrl;
    private double timestamp;
    private VideoControlType type;

    public enum VideoControlType {
        UPDATE_URL, PLAY, PAUSE, SEEK
    }
}
