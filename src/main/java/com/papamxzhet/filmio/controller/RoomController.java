package com.papamxzhet.filmio.controller;

import com.papamxzhet.filmio.model.Room;
import com.papamxzhet.filmio.model.VideoControlMessage;
import com.papamxzhet.filmio.payload.*;
import com.papamxzhet.filmio.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
                        room.getParticipantCount(),
                        room.getAvatarUrl()
                ))
                .toList();
    }

    // Получение комнаты по ID
    @GetMapping("/{id}")
    public RoomResponse getRoomById(@PathVariable UUID id) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Комната не найдена"));

        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getOwner(),
                room.isHasPassword(), // Проверяем, есть ли пароль
                room.isClosed(),
                room.getParticipantCount(),
                room.getAvatarUrl()
        );
    }


    // Проверка пароля для комнаты
    @PostMapping("/{id}/check-password")
    public boolean checkRoomPassword(
            @PathVariable UUID id,
            @RequestBody PasswordCheckRequest request) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Комната не найдена"));
        return roomService.checkRoomPassword(room, request.getPassword());
    }

    // Обновление пароля комнаты
    @PutMapping("/{id}/update-password")
    public Room updateRoomPassword(
            @PathVariable UUID id,
            @RequestBody PasswordUpdateRequest request,
            Authentication authentication) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Комната не найдена"));

        if (!room.getOwner().equals(authentication.getName())) {
            throw new RuntimeException("Вы не являетесь владельцем этой комнаты");
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
                .orElseThrow(() -> new RuntimeException("Комната не найдена"));

        if (!room.getOwner().equals(authentication.getName())) {
            throw new RuntimeException("Вы не являетесь владельцем этой комнаты");
        }

        return roomService.updateRoomName(room, request.getName());
    }


    // Удаление комнаты
    @DeleteMapping("/{id}")
    public void deleteRoom(@PathVariable UUID id, Authentication authentication) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Комната не найдена"));

        if (!room.getOwner().equals(authentication.getName())) {
            throw new RuntimeException("Вы не являетесь владельцем этой комнаты");
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


    @PostMapping("/{id}/video-control")
    public void handleVideoControl(@PathVariable UUID id, @RequestBody VideoControlRequest request) {
        // Получаем текущее состояние комнаты
        VideoControlMessage currentState = roomService.getCurrentVideoState(id).orElse(null);

        // Создаем новое сообщение
        VideoControlMessage message = new VideoControlMessage(
                id.toString(),
                (request.getType().equals("UPDATE_URL")) ? request.getVideoUrl() : (currentState != null ? currentState.getVideoUrl() : null),
                request.getTimestamp(),
                VideoControlMessage.VideoControlType.valueOf(request.getType())
        );

        System.out.println("Получено видео-событие: " + message);

        if (message.getType() == VideoControlMessage.VideoControlType.UPDATE_URL) {
            roomService.updateVideoState(id, message);
        }

        roomService.broadcastVideoControl(message);
    }


    @GetMapping("/{roomId}/video-state")
    public VideoControlMessage getVideoState(@PathVariable UUID roomId) {
        roomService.initializeVideoState(roomId); // Убедиться, что состояние инициализировано
        return roomService.getCurrentVideoState(roomId)
                .orElseThrow(() -> new RuntimeException("Не удалось получить текущее состояние."));
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
        System.out.println("Список участников комнаты после изменений: " + participants);
        messagingTemplate.convertAndSend("/topic/participants/" + roomId, participants);
    }

    @PostMapping("/{id}/remove-participant")
    public ResponseEntity<String> removeParticipant(
            @PathVariable UUID id,
            @RequestBody ParticipantRemoveRequest request,
            Authentication authentication) {
        System.out.println("Запрос на удаление участника: " + request.getUsername());

        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Комната не найдена"));

        if (!room.getOwner().equals(authentication.getName())) {
            throw new RuntimeException("Вы не являетесь владельцем этой комнаты.");
        }

        roomService.removeParticipant(id, request.getUsername());
        System.out.println("Участник удалён: " + request.getUsername());

        return ResponseEntity.ok("Участник успешно удалён.");
    }

    @PostMapping("/{id}/upload-avatar")
    public Room uploadAvatar(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!room.getOwner().equals(authentication.getName())) {
            throw new RuntimeException("You are not the owner of this room");
        }

        String avatarUrl = roomService.saveAvatar(file);
        room.setAvatarUrl(avatarUrl);
        return roomService.updateRoom(room);
    }
}
