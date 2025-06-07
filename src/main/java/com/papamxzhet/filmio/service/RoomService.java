package com.papamxzhet.filmio.service;

import com.papamxzhet.filmio.model.Room;
import com.papamxzhet.filmio.model.VideoControlMessage;
import com.papamxzhet.filmio.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;
    private final ParticipantService participantService;
    private final VideoControlService videoControlService;
    private final MinioService minioService;

    @Autowired
    public RoomService(RoomRepository roomRepository,
                       PasswordEncoder passwordEncoder,
                       ParticipantService participantService,
                       VideoControlService videoControlService,
                       MinioService minioService) {
        this.roomRepository = roomRepository;
        this.passwordEncoder = passwordEncoder;
        this.participantService = participantService;
        this.videoControlService = videoControlService;
        this.minioService = minioService;
    }

    @Transactional
    public Room createRoom(String name, String owner, String rawPassword, boolean isClosed, String description) {
        String encodedPassword = (rawPassword != null && !rawPassword.trim().isEmpty())
                ? passwordEncoder.encode(rawPassword)
                : null;

        Room room = new Room(name, owner, encodedPassword);
        room.setClosed(isClosed);
        room.setDescription(description);

        Room savedRoom = roomRepository.save(room);

        participantService.initializeParticipantsList(savedRoom.getId());
        videoControlService.initializeVideoState(savedRoom.getId());

        return savedRoom;
    }

    public List<Room> getAllRooms() {
        List<Room> rooms = roomRepository.findAll();

        for (Room room : rooms) {
            participantService.initializeParticipantsList(room.getId());
            room.setParticipantCount(participantService.getParticipantCount(room.getId()));
        }

        return rooms;
    }

    public Optional<Room> getRoomById(UUID id) {
        return roomRepository.findById(id);
    }

    public boolean checkRoomPassword(Room room, String rawPassword) {
        return passwordEncoder.matches(rawPassword, room.getPassword());
    }

    @Transactional
    public Room updateRoomPassword(Room room, String newRawPassword) {
        String encodedPassword = passwordEncoder.encode(newRawPassword);
        room.setPassword(encodedPassword);
        return roomRepository.save(room);
    }

    @Transactional
    public Room updateRoomName(Room room, String newName) {
        room.setName(newName);
        return roomRepository.save(room);
    }

    @Transactional
    public Room updateRoomDescription(Room room, String newDescription) {
        room.setDescription(newDescription);
        return roomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(UUID id) {
        roomRepository.deleteById(id);
        participantService.removeRoom(id);
        videoControlService.removeRoomVideoState(id);
    }

    public void addParticipant(UUID roomId, String username) {
        participantService.addParticipant(roomId, username);
        participantService.sendJoinLeaveNotification(roomId, username, true);
    }

    public void removeParticipant(UUID roomId, String username) {
        boolean removed = participantService.removeParticipant(roomId, username);
        if (removed) {
            participantService.sendJoinLeaveNotification(roomId, username, false);
        }
    }

    public int getParticipantCount(UUID roomId) {
        return participantService.getParticipantCount(roomId);
    }

    public boolean forceRemoveParticipant(UUID roomId, String username) {
        return participantService.forceRemoveParticipant(roomId, username);
    }

    public List<String> getParticipants(UUID roomId) {
        return participantService.getParticipants(roomId);
    }

    public Optional<VideoControlMessage> getCurrentVideoState(UUID roomId) {
        return videoControlService.getCurrentVideoState(roomId);
    }

    public void updateVideoState(UUID roomId, VideoControlMessage message) {
        videoControlService.updateVideoState(roomId, message);
    }

    public void broadcastVideoControl(VideoControlMessage message, boolean excludeInitiator) {
        videoControlService.broadcastVideoControl(message, excludeInitiator);
    }

    public String saveAvatar(MultipartFile file) throws Exception {
        String fileKey = minioService.uploadFile(file);
        return minioService.getPresignedUrl(fileKey);
    }

    public String saveCover(MultipartFile file) throws Exception {
        String fileKey = minioService.uploadFile(file);
        return minioService.getPresignedUrl(fileKey);
    }

    @Transactional
    public Room updateRoom(Room room) {
        return roomRepository.save(room);
    }
}