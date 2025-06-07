package com.papamxzhet.filmio.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class VideoControlMessage {
    private String roomId;
    private String videoUrl;
    private double timestamp;
    private VideoControlType type;
    private String initiator;
    private Long sentAt;
    private Boolean playing;
    private Long stateVersion;

    public Boolean isPlaying() {
        return this.playing;
    }

    public double getDuration() {
        return this.timestamp;
    }

    public enum VideoControlType {
        PLAY, PAUSE, SEEK, SYNC_REQUEST, SYNC_RESPONSE, UPDATE_URL, FORCE_SYNC, PING, PONG
    }

    public VideoControlMessage() {
        this.sentAt = System.currentTimeMillis();
        this.stateVersion = 1L;
    }

    public VideoControlMessage(String roomId, String videoUrl, double timestamp, VideoControlType type) {
        this.roomId = roomId;
        this.videoUrl = videoUrl;
        this.timestamp = timestamp;
        this.type = type;
        this.sentAt = System.currentTimeMillis();
        this.playing = type == VideoControlType.PLAY;
        this.stateVersion = 1L;
    }

    public VideoControlMessage(String roomId, String videoUrl, double timestamp, VideoControlType type, String initiator) {
        this.roomId = roomId;
        this.videoUrl = videoUrl;
        this.timestamp = timestamp;
        this.type = type;
        this.initiator = initiator;
        this.sentAt = System.currentTimeMillis();
        this.playing = type == VideoControlType.PLAY;
        this.stateVersion = 1L;
    }

    public boolean isValid() {
        return roomId != null && !roomId.isEmpty() && type != null;
    }
}