package com.papamxzhet.filmio.controller;

import com.papamxzhet.filmio.model.Room;
import com.papamxzhet.filmio.model.VideoControlMessage;
import com.papamxzhet.filmio.payload.*;
import com.papamxzhet.filmio.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Создание комнаты
    @PostMapping("/create")
    public Room createRoom(
            @RequestBody RoomCreationRequest request,
            Authentication authentication) {
        String owner = authentication.getName();
        return roomService.createRoom(
                request.getName(),
                owner,
                request.getPassword(),
                request.isClosed()
        );
    }


    // Получение списка всех комнат
    @GetMapping
    public List<RoomResponse> getAllRooms() {
        List<Room> rooms = roomService.getAllRooms();
        return rooms.stream()
                .map(room -> new RoomResponse(
                        room.getId(),
                        room.getName(),
                        room.getOwner(),
                        room.isHasPassword(), // Проверяем наличие пароля
                        room.isClosed(),
                        room.getParticipantCount()
                ))
                .toList();
    }

    // Получение комнаты по ID
    @GetMapping("/{id}")
    public RoomResponse getRoomById(@PathVariable UUID id) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getOwner(),
                room.isHasPassword(), // Проверяем, есть ли пароль
                room.isClosed(),
                room.getParticipantCount()
        );
    }


    // Проверка пароля для комнаты
    @PostMapping("/{id}/check-password")
    public boolean checkRoomPassword(
            @PathVariable UUID id,
            @RequestBody PasswordCheckRequest request) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        return roomService.checkRoomPassword(room, request.getPassword());
    }

    // Обновление пароля комнаты
    @PutMapping("/{id}/update-password")
    public Room updateRoomPassword(
            @PathVariable UUID id,
            @RequestBody PasswordUpdateRequest request,
            Authentication authentication) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!room.getOwner().equals(authentication.getName())) {
            throw new RuntimeException("You are not the owner of this room");
        }

        return roomService.updateRoomPassword(room, request.getNewPassword());
    }

    // Обновление имени комнаты
    @PutMapping("/{id}/update-name")
    public Room updateRoomName(
            @PathVariable UUID id,
            @RequestBody RoomNameUpdateRequest request,
            Authentication authentication) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!room.getOwner().equals(authentication.getName())) {
            throw new RuntimeException("You are not the owner of this room");
        }

        return roomService.updateRoomName(room, request.getName());
    }


    // Удаление комнаты
    @DeleteMapping("/{id}")
    public void deleteRoom(@PathVariable UUID id, Authentication authentication) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!room.getOwner().equals(authentication.getName())) {
            throw new RuntimeException("You are not the owner of this room");
        }

        roomService.deleteRoom(id);
    }

    @PostMapping("/{id}/join")
    public void joinRoom(@PathVariable UUID id, Authentication authentication) {
        String username = authentication.getName();
        roomService.addParticipant(id, username);
        notifyParticipantsChange(id);
        sendJoinLeaveNotification(id, username, true);
    }

    @PostMapping("/{id}/leave")
    public void leaveRoom(@PathVariable UUID id, Authentication authentication) {
        String username = authentication.getName();
        roomService.removeParticipant(id, username);
        notifyParticipantsChange(id);
        sendJoinLeaveNotification(id, username, false);
    }


    private void sendJoinLeaveNotification(UUID roomId, String username, boolean isJoining) {
        String message = isJoining
                ? username + " joined the room."
                : username + " left the room.";
        messagingTemplate.convertAndSend("/topic/" + roomId + "/notifications", message);
    }


    // Новый endpoint для управления видео
    @PostMapping("/{id}/video-control")
    public void handleVideoControl(
            @PathVariable UUID id,
            @RequestBody VideoControlRequest request) {

        VideoControlMessage message = new VideoControlMessage();
        message.setRoomId(id.toString());
        message.setVideoUrl(request.getVideoUrl());
        message.setTimestamp(request.getTimestamp());
        message.setType(VideoControlMessage.VideoControlType.valueOf(request.getType()));

        roomService.broadcastVideoControl(message);
    }

    @PostMapping("/{roomId}/seek")
    public void seekVideo(@PathVariable UUID roomId, @RequestBody SeekRequest request) {
        VideoControlMessage message = new VideoControlMessage();
        message.setRoomId(roomId.toString());
        message.setTimestamp(request.getTimestamp());
        message.setType(VideoControlMessage.VideoControlType.SEEK);

        // Broadcast the seek event to all participants
        roomService.broadcastVideoControl(message);
    }

    // Уведомление участников о изменениях
    private void notifyParticipantsChange(UUID roomId) {
        List<String> participants = roomService.getParticipants(roomId);
        messagingTemplate.convertAndSend("/topic/participants/" + roomId, participants);
    }

    @PostMapping("/{id}/remove-participant")
    public void removeParticipant(@PathVariable UUID id, @RequestBody ParticipantRemoveRequest request, Authentication authentication) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!room.getOwner().equals(authentication.getName())) {
            throw new RuntimeException("You are not the owner of this room");
        }

        roomService.removeParticipant(id, request.getUsername());
        notifyParticipantsChange(id);
    }

}
