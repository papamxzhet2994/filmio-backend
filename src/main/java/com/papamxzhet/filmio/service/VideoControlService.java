package com.papamxzhet.filmio.service;

import com.papamxzhet.filmio.model.VideoControlMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class VideoControlService {
    private static final Logger logger = LoggerFactory.getLogger(VideoControlService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final ConcurrentHashMap<UUID, VideoControlMessage> videoStateMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, AtomicLong> stateVersionMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Map<String, Long>> lastOperationTimestampMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, String>> lastMessageIdMap = new ConcurrentHashMap<>();

    private static final long MIN_UPDATE_INTERVAL_MS = 300;
    private static final long SYNC_REQUEST_COOLDOWN_MS = 1000;
    private static final long FORCE_SYNC_INTERVAL_MS = 10000;

    private final ConcurrentHashMap<UUID, Long> lastForceSyncMap = new ConcurrentHashMap<>();

    public void initializeVideoState(UUID roomId) {
        videoStateMap.putIfAbsent(roomId, new VideoControlMessage(
                roomId.toString(),
                "",
                0,
                VideoControlMessage.VideoControlType.PAUSE
        ));
        stateVersionMap.putIfAbsent(roomId, new AtomicLong(1));
        lastOperationTimestampMap.putIfAbsent(roomId, new ConcurrentHashMap<>());
        lastMessageIdMap.putIfAbsent(roomId, new ConcurrentHashMap<>());
        lastForceSyncMap.putIfAbsent(roomId, System.currentTimeMillis());
    }

    public Optional<VideoControlMessage> getCurrentVideoState(UUID roomId) {
        VideoControlMessage state = videoStateMap.get(roomId);
        if (state != null) {
            VideoControlMessage stateCopy = new VideoControlMessage(
                    state.getRoomId(),
                    state.getVideoUrl(),
                    state.getTimestamp(),
                    state.getType(),
                    state.getInitiator()
            );
            stateCopy.setPlaying(state.isPlaying());
            stateCopy.setSentAt(System.currentTimeMillis());

            stateCopy.setStateVersion(stateVersionMap.get(roomId).get());

            return Optional.of(stateCopy);
        }
        return Optional.empty();
    }

    public boolean updateVideoState(UUID roomId, VideoControlMessage message) {
        String initiator = message.getInitiator();

        if (initiator == null || shouldThrottleRequest(roomId, initiator, message.getType())) {
            return false;
        }

        String messageId = generateMessageId(message);
        if (isDuplicateMessage(roomId, initiator, messageId)) {
            return false;
        }

        recordMessageId(roomId, initiator, messageId);

        updateLastOperationTimestamp(roomId, initiator, message.getType());

        videoStateMap.compute(roomId, (key, currentState) -> {
            if (currentState == null) {
                message.setStateVersion(stateVersionMap.computeIfAbsent(roomId, k -> new AtomicLong(1)).getAndIncrement());
                return message;
            }

            if (message.getVideoUrl() != null && !message.getVideoUrl().isEmpty()
                    && !message.getVideoUrl().equals(currentState.getVideoUrl())) {
                currentState.setTimestamp(0);
                currentState.setVideoUrl(message.getVideoUrl());
                currentState.setPlaying(false);
            } else if (message.getTimestamp() >= 0) {
                if (message.getType() == VideoControlMessage.VideoControlType.SEEK) {
                    currentState.setTimestamp(message.getTimestamp());
                } else if (message.getType() == VideoControlMessage.VideoControlType.PLAY ||
                        message.getType() == VideoControlMessage.VideoControlType.PAUSE) {
                    double timeDiff = Math.abs(message.getTimestamp() - currentState.getTimestamp());
                    if (timeDiff > 1.0) {
                        currentState.setTimestamp(message.getTimestamp());
                    }
                }
            }

            currentState.setType(message.getType());

            if (message.getType() == VideoControlMessage.VideoControlType.PLAY ||
                    message.getType() == VideoControlMessage.VideoControlType.PAUSE) {
                currentState.setPlaying(message.getType() == VideoControlMessage.VideoControlType.PLAY);
            } else if (message.isPlaying() != null) {
                currentState.setPlaying(message.isPlaying());
            }

            currentState.setSentAt(System.currentTimeMillis());

            currentState.setInitiator(message.getInitiator());

            currentState.setStateVersion(stateVersionMap.get(roomId).getAndIncrement());

            return currentState;
        });

        return true;
    }

    public boolean shouldForceSynchronization(UUID roomId) {
        long lastForceSyncTime = lastForceSyncMap.getOrDefault(roomId, 0L);
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastForceSyncTime > FORCE_SYNC_INTERVAL_MS) {
            lastForceSyncMap.put(roomId, currentTime);
            return true;
        }

        return false;
    }

    private String generateMessageId(VideoControlMessage message) {
        return message.getType() + "-" + message.getTimestamp() + "-" + message.getSentAt();
    }

    private boolean isDuplicateMessage(UUID roomId, String initiator, String messageId) {
        Map<String, String> lastMessages = lastMessageIdMap.get(roomId);
        if (lastMessages == null) {
            return false;
        }

        String lastMsgId = lastMessages.get(initiator);
        return messageId.equals(lastMsgId);
    }

    private void recordMessageId(UUID roomId, String initiator, String messageId) {
        lastMessageIdMap.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                .put(initiator, messageId);
    }

    private boolean shouldThrottleRequest(UUID roomId, String initiator, VideoControlMessage.VideoControlType type) {
        Map<String, Long> lastOperationMap = lastOperationTimestampMap.get(roomId);
        if (lastOperationMap == null) {
            return false;
        }

        Long lastTimestamp = lastOperationMap.get(initiator);
        if (lastTimestamp == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();

        if (type == VideoControlMessage.VideoControlType.SYNC_REQUEST) {
            return (currentTime - lastTimestamp) < SYNC_REQUEST_COOLDOWN_MS;
        } else if (type == VideoControlMessage.VideoControlType.FORCE_SYNC) {
            return (currentTime - lastTimestamp) < FORCE_SYNC_INTERVAL_MS;
        } else {
            return (currentTime - lastTimestamp) < MIN_UPDATE_INTERVAL_MS;
        }
    }

    private void updateLastOperationTimestamp(UUID roomId, String initiator, VideoControlMessage.VideoControlType type) {
        if (initiator == null) {
            return;
        }

        lastOperationTimestampMap.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                .put(initiator, System.currentTimeMillis());
    }

    public void broadcastVideoControl(VideoControlMessage message, boolean excludeInitiator) {
        String destination = "/topic/video/" + message.getRoomId();

        message.setSentAt(System.currentTimeMillis());

        if (excludeInitiator && message.getInitiator() != null) {
            messagingTemplate.convertAndSend(
                    destination,
                    message,
                    Collections.singletonMap("excludeUser", message.getInitiator())
            );
        } else {
            messagingTemplate.convertAndSend(destination, message);
        }

        logger.debug("Broadcast video control message: {}", message);
    }

    public void forceSynchronize(UUID roomId) {
        VideoControlMessage currentState = videoStateMap.get(roomId);
        if (currentState != null) {
            VideoControlMessage syncMessage = new VideoControlMessage(
                    currentState.getRoomId(),
                    currentState.getVideoUrl(),
                    currentState.getTimestamp(),
                    VideoControlMessage.VideoControlType.FORCE_SYNC
            );
            syncMessage.setPlaying(currentState.isPlaying());
            syncMessage.setSentAt(System.currentTimeMillis());
            syncMessage.setInitiator("SERVER");
            syncMessage.setStateVersion(stateVersionMap.get(roomId).get());

            broadcastVideoControl(syncMessage, false);

            lastForceSyncMap.put(roomId, System.currentTimeMillis());
        }
    }

    public void removeRoomVideoState(UUID roomId) {
        videoStateMap.remove(roomId);
        stateVersionMap.remove(roomId);
        lastOperationTimestampMap.remove(roomId);
        lastMessageIdMap.remove(roomId);
        lastForceSyncMap.remove(roomId);
    }
}