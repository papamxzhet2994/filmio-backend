package com.papamxzhet.filmio.controller;

import com.papamxzhet.filmio.model.Room;
import com.papamxzhet.filmio.model.VideoControlMessage;
import com.papamxzhet.filmio.payload.*;
import com.papamxzhet.filmio.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    private RoomWebSocketController roomWebSocketController;

    @PostMapping("/create")
    public Room createRoom(
            @RequestBody RoomCreationRequest request,
            Authentication authentication) {
        String owner = authentication.getName();
        roomWebSocketController.notifyRoomsUpdate();
        return roomService.createRoom(
                request.getName(),
                owner,
                request.getPassword(),
                request.isClosed(),
                request.getDescription()
        );
    }

    @GetMapping
    public List<RoomResponse> getAllRooms() {
        List<Room> rooms = roomService.getAllRooms();
        return rooms.stream()
                .map(room -> new RoomResponse(
                        room.getId(),
                        room.getName(),
                        room.getOwner(),
                        room.isHasPassword(),
                        room.isClosed(),
                        room.getParticipantCount(),
                        room.getAvatarUrl(),
                        room.getCoverUrl(),
                        room.getDescription(),
                        room.getCreatedAt()
                ))
                .toList();
    }

    @GetMapping("/{id}")
    public RoomResponse getRoomById(@PathVariable UUID id) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getOwner(),
                room.isHasPassword(),
                room.isClosed(),
                room.getParticipantCount(),
                room.getAvatarUrl(),
                room.getCoverUrl(),
                room.getDescription(),
                room.getCreatedAt()
        );
    }

    @PostMapping("/{id}/check-password")
    public boolean checkRoomPassword(
            @PathVariable UUID id,
            @RequestBody PasswordCheckRequest request) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Комната не найдена"));
        return roomService.checkRoomPassword(room, request.getPassword());
    }

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

        roomWebSocketController.notifyRoomsUpdate();
        return roomService.updateRoomPassword(room, request.getNewPassword());
    }

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

        roomWebSocketController.notifyRoomsUpdate();

        return roomService.updateRoomName(room, request.getName());
    }

    @PutMapping("/{id}/update-description")
    public Room updateRoomDescription(
            @PathVariable UUID id,
            @RequestBody DescriptionUpdateRequest request,
            Authentication authentication) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!room.getOwner().equals(authentication.getName())) {
            throw new RuntimeException("You are not the owner of this room");
        }

        roomWebSocketController.notifyRoomsUpdate();

        return roomService.updateRoomDescription(room, request.getDescription());
    }

    @DeleteMapping("/{id}")
    public void deleteRoom(@PathVariable UUID id, Authentication authentication) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Комната не найдена"));

        if (!room.getOwner().equals(authentication.getName())) {
            throw new RuntimeException("Вы не являетесь владельцем этой комнаты");
        }

        roomWebSocketController.notifyRoomsUpdate();
        roomService.deleteRoom(id);
    }

    @PostMapping("/{id}/join")
    public void joinRoom(@PathVariable UUID id, Authentication authentication) {
        String username = authentication.getName();
        roomService.addParticipant(id, username);
    }

    @PostMapping("/{id}/leave")
    public void leaveRoom(@PathVariable UUID id, Authentication authentication) {
        String username = authentication.getName();
        roomService.removeParticipant(id, username);
    }

    @PostMapping("/{id}/video-control")
    public void handleVideoControl(
            @PathVariable UUID id,
            @RequestBody VideoControlRequest request,
            Authentication authentication
    ) {
        VideoControlMessage message = new VideoControlMessage(
                id.toString(),
                request.getVideoUrl(),
                request.getTimestamp(),
                VideoControlMessage.VideoControlType.valueOf(request.getType()),
                authentication.getName()
        );

        roomService.updateVideoState(id, message);

        roomService.broadcastVideoControl(message, true);
    }

    @GetMapping("/{roomId}/video-state")
    public VideoControlMessage getVideoState(@PathVariable UUID roomId) {
        return roomService.getCurrentVideoState(roomId)
                .orElseGet(() -> {
                    VideoControlMessage initialState = new VideoControlMessage(
                            roomId.toString(),
                            "",
                            0,
                            VideoControlMessage.VideoControlType.PAUSE
                    );
                    roomService.updateVideoState(roomId, initialState);
                    return initialState;
                });
    }

    @PostMapping("/{roomId}/seek")
    public void seekVideo(
            @PathVariable UUID roomId,
            @RequestBody SeekRequest request,
            Authentication authentication) {
        VideoControlMessage message = new VideoControlMessage();
        message.setRoomId(roomId.toString());
        message.setTimestamp(request.getTimestamp());
        message.setType(VideoControlMessage.VideoControlType.SEEK);
        message.setInitiator(authentication.getName());

        roomService.broadcastVideoControl(message, true);
    }

    @PostMapping("/{id}/remove-participant")
    public ResponseEntity<String> removeParticipant(
            @PathVariable UUID id,
            @RequestBody ParticipantRemoveRequest request,
            Authentication authentication) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!room.getOwner().equals(authentication.getName())) {
            throw new RuntimeException("You are not the owner of this room");
        }

        roomService.forceRemoveParticipant(id, request.getUsername());
        return ResponseEntity.ok("Participant successfully removed");
    }


    @PostMapping("/{id}/upload-avatar")
    public Room uploadAvatar(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws Exception {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Комната не найдена"));

        if (!room.getOwner().equals(authentication.getName())) {
            throw new RuntimeException("Вы не являетесь владельцем этой комнаты");
        }

        String avatarUrl = roomService.saveAvatar(file);
        room.setAvatarUrl(avatarUrl);
        roomWebSocketController.notifyRoomsUpdate();
        return roomService.updateRoom(room);
    }

    @PostMapping("/{id}/upload-cover")
    public Room uploadCover(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws Exception {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (!room.getOwner().equals(authentication.getName())) {
            throw new RuntimeException("You are not the owner of this room");
        }

        String coverUrl = roomService.saveCover(file);
        room.setCoverUrl(coverUrl);
        roomWebSocketController.notifyRoomsUpdate();
        return roomService.updateRoom(room);
    }
}