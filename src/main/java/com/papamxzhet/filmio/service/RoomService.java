package com.papamxzhet.filmio.service;

import com.papamxzhet.filmio.model.Room;
import com.papamxzhet.filmio.model.VideoControlMessage;
import com.papamxzhet.filmio.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<String>> participantsMap = new ConcurrentHashMap<>();

    @Transactional
    public Room createRoom(String name, String owner, String rawPassword, boolean isClosed) {
        String encodedPassword = (rawPassword != null && !rawPassword.trim().isEmpty())
                ? passwordEncoder.encode(rawPassword)
                : null;

        Room room = new Room(name, owner, encodedPassword);
        room.setClosed(isClosed);
        return roomRepository.save(room);
    }


    public List<Room> getAllRooms() {
        List<Room> rooms = roomRepository.findAll();
        for (Room room : rooms) {
            room.setParticipantCount(getParticipants(room.getId()).size());
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
    public void deleteRoom(UUID id) {
        roomRepository.deleteById(id);
        participantsMap.remove(id);
    }

    public void addParticipant(UUID roomId, String username) {
        participantsMap.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).addIfAbsent(username);
        notifyParticipantsChange(roomId);
    }

    public void removeParticipant(UUID roomId, String username) {
        List<String> participants = participantsMap.get(roomId);
        if (participants != null && participants.remove(username)) {
            notifyParticipantsChange(roomId);
        }
    }

    public List<String> getParticipants(UUID roomId) {
        return participantsMap.getOrDefault(roomId, new CopyOnWriteArrayList<>());
    }

    private void notifyParticipantsChange(UUID roomId) {
        List<String> participants = getParticipants(roomId);
        messagingTemplate.convertAndSend("/topic/participants/" + roomId, participants);
        System.out.println("Уведомление отправлено в /topic/participants/" + roomId + " с участниками: " + participants);
    }

    public void broadcastVideoControl(VideoControlMessage message) {
        messagingTemplate.convertAndSend("/topic/video/" + message.getRoomId(), message);
    }
}
