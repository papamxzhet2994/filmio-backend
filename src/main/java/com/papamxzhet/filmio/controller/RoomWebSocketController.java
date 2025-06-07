package com.papamxzhet.filmio.controller;

import com.papamxzhet.filmio.event.RoomUpdateEvent;
import com.papamxzhet.filmio.model.ParticipantMessage;
import com.papamxzhet.filmio.model.Room;
import com.papamxzhet.filmio.payload.ParticipantRemoveRequest;
import com.papamxzhet.filmio.payload.RoomResponse;
import com.papamxzhet.filmio.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class RoomWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RoomService roomService;

    @MessageMapping("/join")
    public void join(ParticipantMessage message) {
        String username = message.getUsername();
        roomService.addParticipant(message.getRoomId(), username);
    }

    @MessageMapping("/leave")
    public void leave(ParticipantMessage message) {
        String username = message.getUsername();
        roomService.removeParticipant(message.getRoomId(), username);
    }

    @MessageMapping("/remove-participant/{roomId}")
    public void removeParticipant(
            @DestinationVariable UUID roomId,
            ParticipantRemoveRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        try {
            Principal user = headerAccessor.getUser();
            if (user == null) {
                System.err.println("Не удалось получить информацию о пользователе из WebSocket");
                return;
            }

            String currentUsername = user.getName();
            System.out.println("Попытка удаления участника. Инициатор: " + currentUsername + ", Удаляемый: " + request.getUsername());

            Room room = roomService.getRoomById(roomId)
                    .orElseThrow(() -> new RuntimeException("Room not found"));

            if (!room.getOwner().equals(currentUsername)) {
                System.err.println("Пользователь " + currentUsername + " не является владельцем комнаты " + roomId);
                messagingTemplate.convertAndSendToUser(
                        currentUsername,
                        "/queue/error",
                        "Вы не являетесь владельцем этой комнаты"
                );
                return;
            }

            boolean removed = roomService.forceRemoveParticipant(roomId, request.getUsername());

            if (removed) {
                System.out.println("Участник " + request.getUsername() + " успешно удален из комнаты " + roomId);
                messagingTemplate.convertAndSendToUser(
                        currentUsername,
                        "/queue/success",
                        "Участник " + request.getUsername() + " успешно удален"
                );
            } else {
                System.err.println("Не удалось удалить участника " + request.getUsername() + " из комнаты " + roomId);
                messagingTemplate.convertAndSendToUser(
                        currentUsername,
                        "/queue/error",
                        "Не удалось удалить участника"
                );
            }

        } catch (Exception e) {
            System.err.println("Ошибка при удалении участника: " + e.getMessage());
            e.printStackTrace();

            try {
                Principal user = headerAccessor.getUser();
                if (user != null) {
                    messagingTemplate.convertAndSendToUser(
                            user.getName(),
                            "/queue/error",
                            "Произошла ошибка при удалении участника: " + e.getMessage()
                    );
                }
            } catch (Exception sendError) {
                System.err.println("Не удалось отправить сообщение об ошибке: " + sendError.getMessage());
            }
        }
    }

    @EventListener
    public void handleRoomUpdate(RoomUpdateEvent event) {
        notifyRoomsUpdate();
    }

    @MessageMapping("/rooms")
    @SendTo("/topic/rooms")
    public List<RoomResponse> sendRoomUpdate() {
        List<Room> rooms = roomService.getAllRooms();
        return rooms.stream()
                .map(room -> new RoomResponse(
                        room.getId(),
                        room.getName(),
                        room.getOwner(),
                        room.isHasPassword(),
                        room.isClosed(),
                        roomService.getParticipantCount(room.getId()),
                        room.getAvatarUrl(),
                        room.getCoverUrl(),
                        room.getDescription(),
                        room.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    public void notifyRoomsUpdate() {
        messagingTemplate.convertAndSend("/topic/rooms", sendRoomUpdate());
    }
}