package com.papamxzhet.filmio.service;

import com.papamxzhet.filmio.event.RoomUpdateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ParticipantService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<String>> participantsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UUID> userToRoomMap = new ConcurrentHashMap<>();

    public void initializeParticipantsList(UUID roomId) {
        participantsMap.putIfAbsent(roomId, new CopyOnWriteArrayList<>());
    }

    public void addParticipant(UUID roomId, String username) {
        participantsMap.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).addIfAbsent(username);
        userToRoomMap.put(username, roomId);
        notifyParticipantsChange(roomId);
    }

    public boolean removeParticipant(UUID roomId, String username) {
        List<String> participants = participantsMap.get(roomId);

        if (participants == null) {
            throw new RuntimeException("Room participants list not found");
        }

        boolean removed = participants.remove(username);
        if (removed) {
            userToRoomMap.remove(username);
            notifyParticipantsChange(roomId);
        }

        return removed;
    }

    public boolean forceRemoveParticipant(UUID roomId, String username) {
        List<String> participants = participantsMap.get(roomId);

        if (participants == null) {
            throw new RuntimeException("Room participants list not found");
        }

        boolean removed = participants.remove(username);
        if (removed) {
            userToRoomMap.remove(username);

            notifyUserRemoved(username);

            notifyUserRemovedFromRoom(roomId, username);

            sendJoinLeaveNotification(roomId, username, false);

            notifyParticipantsChange(roomId);
        }

        return removed;
    }

    public List<String> getParticipants(UUID roomId) {
        return participantsMap.getOrDefault(roomId, new CopyOnWriteArrayList<>());
    }

    public int getParticipantCount(UUID roomId) {
        return getParticipants(roomId).size();
    }

    public void removeRoom(UUID roomId) {
        List<String> participants = participantsMap.get(roomId);
        if (participants != null) {
            for (String participant : participants) {
                userToRoomMap.remove(participant);
            }
        }
        participantsMap.remove(roomId);
    }

    public void sendJoinLeaveNotification(UUID roomId, String username, boolean isJoining) {
        String message = isJoining
                ? username + " присоединился(ась) к комнате."
                : username + " покинул(а) комнату.";
        messagingTemplate.convertAndSend("/topic/" + roomId + "/notifications", message);
    }

    private void notifyParticipantsChange(UUID roomId) {
        List<String> participants = getParticipants(roomId);
        messagingTemplate.convertAndSend("/topic/participants/" + roomId, participants);

        eventPublisher.publishEvent(new RoomUpdateEvent(this, roomId));
    }

    private void notifyUserRemoved(String username) {
        try {
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/removed",
                    "Вы были удалены из комнаты владельцем."
            );
        } catch (Exception e) {
            System.err.println("Не удалось отправить персональное уведомление пользователю: " + username);
            e.printStackTrace();
        }
    }

    private void notifyUserRemovedFromRoom(UUID roomId, String username) {
        messagingTemplate.convertAndSend("/topic/" + roomId + "/user-removed", username);
    }
}