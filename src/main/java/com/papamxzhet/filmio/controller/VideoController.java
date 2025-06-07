package com.papamxzhet.filmio.controller;

import com.papamxzhet.filmio.model.VideoControlMessage;
import com.papamxzhet.filmio.service.VideoControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class VideoController {
    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final VideoControlService videoControlService;

    private final ConcurrentHashMap<UUID, Long> activeRooms = new ConcurrentHashMap<>();
    private static final long ROOM_ACTIVITY_TIMEOUT_MS = 3600000;

    public VideoController(SimpMessagingTemplate messagingTemplate,
                           VideoControlService videoControlService) {
        this.messagingTemplate = messagingTemplate;
        this.videoControlService = videoControlService;
    }

    @MessageMapping("/video/{roomId}")
    public void handleVideoControl(@DestinationVariable String roomId, @Payload VideoControlMessage message) {
        logger.debug("Received video control message for room {}: {}", roomId, message);

        if (message.getRoomId() == null || message.getRoomId().isEmpty()) {
            message.setRoomId(roomId);
        } else if (!message.getRoomId().equals(roomId)) {
            logger.warn("Message roomId inconsistent with path. Path: {}, Message: {}",
                    roomId, message.getRoomId());
            message.setRoomId(roomId);
        }

        try {
            UUID roomUuid = UUID.fromString(message.getRoomId());

            activeRooms.put(roomUuid, System.currentTimeMillis());

            if (videoControlService.getCurrentVideoState(roomUuid).isEmpty()) {
                videoControlService.initializeVideoState(roomUuid);
            }

            switch (message.getType()) {
                case SYNC_REQUEST:
                    handleSyncRequest(roomUuid, message);
                    break;

                case FORCE_SYNC:
                    videoControlService.forceSynchronize(roomUuid);
                    break;

                default:
                    boolean updated = videoControlService.updateVideoState(roomUuid, message);
                    if (updated) {
                        logger.debug("Broadcasting video control: {}", message);
                        videoControlService.broadcastVideoControl(message, true);

                        if (videoControlService.shouldForceSynchronization(roomUuid)) {
                            logger.debug("Triggering periodic forced sync for room: {}", roomUuid);
                            videoControlService.forceSynchronize(roomUuid);
                        }
                    } else {
                        logger.debug("Video state update was rejected: {}", message);
                    }
                    break;
            }
        } catch (IllegalArgumentException e) {
            logger.error("Invalid UUID format for roomId: {}", message.getRoomId(), e);
        }
    }

    private void handleSyncRequest(UUID roomId, VideoControlMessage message) {
        if (message.getInitiator() != null) {
            videoControlService.getCurrentVideoState(roomId).ifPresent(state -> {
                logger.debug("Sending sync response to {}: {}", message.getInitiator(), state);

                state.setType(VideoControlMessage.VideoControlType.SYNC_RESPONSE);

                messagingTemplate.convertAndSendToUser(
                        message.getInitiator(),
                        "/queue/video-sync",
                        state
                );
            });
        } else {
            logger.warn("Sync request received with null initiator for room: {}", roomId);
        }
    }

    @Scheduled(fixedRate = 300000)
    public void cleanupInactiveRooms() {
        long currentTime = System.currentTimeMillis();
        Set<UUID> roomsToRemove = new HashSet<>();

        activeRooms.forEach((roomId, lastActivity) -> {
            if (currentTime - lastActivity > ROOM_ACTIVITY_TIMEOUT_MS) {
                roomsToRemove.add(roomId);
            }
        });

        roomsToRemove.forEach(roomId -> {
            logger.info("Removing inactive room: {}", roomId);
            activeRooms.remove(roomId);
            videoControlService.removeRoomVideoState(roomId);
        });
    }

    @Scheduled(fixedRate = 30000)
    public void periodicForcedSync() {
        long currentTime = System.currentTimeMillis();

        activeRooms.forEach((roomId, lastActivity) -> {
            if (currentTime - lastActivity < 600000) {
                videoControlService.forceSynchronize(roomId);
            }
        });
    }
}