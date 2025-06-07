package com.papamxzhet.filmio;

import com.papamxzhet.filmio.model.Room;
import com.papamxzhet.filmio.model.VideoControlMessage;
import com.papamxzhet.filmio.repository.RoomRepository;
import com.papamxzhet.filmio.service.MinioService;
import com.papamxzhet.filmio.service.ParticipantService;
import com.papamxzhet.filmio.service.RoomService;
import com.papamxzhet.filmio.service.VideoControlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ParticipantService participantService;

    @Mock
    private VideoControlService videoControlService;

    @Mock
    private MinioService minioService;

    @InjectMocks
    private RoomService roomService;

    private Room testRoom;
    private UUID testRoomId;
    private String testOwner;
    private String testName;
    private String testPassword;
    private String testDescription;

    @BeforeEach
    void setUp() {
        testRoomId = UUID.randomUUID();
        testOwner = "testOwner";
        testName = "Test Room";
        testPassword = "testPassword";
        testDescription = "Test Description";

        testRoom = new Room(testName, testOwner, "encodedPassword");
        testRoom.setId(testRoomId);
        testRoom.setDescription(testDescription);
        testRoom.setClosed(false);
    }

    @DisplayName("Должна создаться новая комната с паролем.")
    @Test
    void createRoom_WithPassword_ShouldCreateRoomSuccessfully() {
        String encodedPassword = "encodedPassword123";
        when(passwordEncoder.encode(testPassword)).thenReturn(encodedPassword);
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

        Room result = roomService.createRoom(testName, testOwner, testPassword, false, testDescription);

        assertNotNull(result);
        assertEquals(testName, result.getName());
        assertEquals(testOwner, result.getOwner());
        assertEquals(testDescription, result.getDescription());
        assertFalse(result.isClosed());

        verify(passwordEncoder).encode(testPassword);
        verify(roomRepository).save(any(Room.class));
        verify(participantService).initializeParticipantsList(testRoom.getId());
        verify(videoControlService).initializeVideoState(testRoom.getId());
    }

    @Test
    @DisplayName("Должна создаться новая комната без пароля.")
    void createRoom_WithoutPassword_ShouldCreateRoomWithNullPassword() {
        Room roomWithoutPassword = new Room(testName, testOwner, null);
        roomWithoutPassword.setId(testRoomId);
        roomWithoutPassword.setDescription(testDescription);
        roomWithoutPassword.setClosed(true);

        when(roomRepository.save(any(Room.class))).thenReturn(roomWithoutPassword);

        Room result = roomService.createRoom(testName, testOwner, null, true, testDescription);

        assertNotNull(result);
        assertEquals(testName, result.getName());
        assertEquals(testOwner, result.getOwner());
        assertTrue(result.isClosed());

        verify(passwordEncoder, never()).encode(anyString());
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    @DisplayName("Должна создаться новая комната с пустым паролем.")
    void createRoom_WithEmptyPassword_ShouldCreateRoomWithNullPassword() {
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

        Room result = roomService.createRoom(testName, testOwner, "   ", false, testDescription);

        assertNotNull(result);
        verify(passwordEncoder, never()).encode(anyString());
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    @DisplayName("Должно вернуть все комнаты.")
    void getAllRooms_ShouldReturnAllRoomsWithParticipantCount() {
        Room room1 = new Room("Room 1", "Owner 1", null);
        room1.setId(UUID.randomUUID());
        Room room2 = new Room("Room 2", "Owner 2", "password");
        room2.setId(UUID.randomUUID());

        List<Room> rooms = Arrays.asList(room1, room2);
        when(roomRepository.findAll()).thenReturn(rooms);
        when(participantService.getParticipantCount(room1.getId())).thenReturn(5);
        when(participantService.getParticipantCount(room2.getId())).thenReturn(3);

        List<Room> result = roomService.getAllRooms();

        assertEquals(2, result.size());
        assertEquals(5, result.get(0).getParticipantCount());
        assertEquals(3, result.get(1).getParticipantCount());

        verify(participantService).initializeParticipantsList(room1.getId());
        verify(participantService).initializeParticipantsList(room2.getId());
    }

    @Test
    @DisplayName("Должно вернуть комнату по ID.")
    void getRoomById_WhenRoomExists_ShouldReturnRoom() {
        when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(testRoom));

        Optional<Room> result = roomService.getRoomById(testRoomId);

        assertTrue(result.isPresent());
        assertEquals(testRoom, result.get());
        verify(roomRepository).findById(testRoomId);
    }

    @Test
    @DisplayName("Должно вернуть что комната не существует.")
    void getRoomById_WhenRoomDoesNotExist_ShouldReturnEmpty() {
        when(roomRepository.findById(testRoomId)).thenReturn(Optional.empty());

        Optional<Room> result = roomService.getRoomById(testRoomId);

        assertFalse(result.isPresent());
        verify(roomRepository).findById(testRoomId);
    }

    @Test
    @DisplayName("Должно вернуть true, если пароль совпадает.")
    void checkRoomPassword_WhenPasswordMatches_ShouldReturnTrue() {
        String rawPassword = "testPassword";
        String encodedPassword = "encodedPassword";
        testRoom.setPassword(encodedPassword);

        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        boolean result = roomService.checkRoomPassword(testRoom, rawPassword);

        assertTrue(result);
        verify(passwordEncoder).matches(rawPassword, encodedPassword);
    }

    @Test
    @DisplayName("Должно вернуть false, если пароль не совпадает.")
    void checkRoomPassword_WhenPasswordDoesNotMatch_ShouldReturnFalse() {
        String rawPassword = "wrongPassword";
        String encodedPassword = "encodedPassword";
        testRoom.setPassword(encodedPassword);

        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        boolean result = roomService.checkRoomPassword(testRoom, rawPassword);

        assertFalse(result);
        verify(passwordEncoder).matches(rawPassword, encodedPassword);
    }

    @Test
    @DisplayName("Должно обновить пароль комнаты.")
    void updateRoomPassword_ShouldUpdatePasswordSuccessfully() {
        String newPassword = "newPassword";
        String encodedNewPassword = "encodedNewPassword";

        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
        when(roomRepository.save(testRoom)).thenReturn(testRoom);

        Room result = roomService.updateRoomPassword(testRoom, newPassword);

        assertNotNull(result);
        assertEquals(encodedNewPassword, testRoom.getPassword());
        verify(passwordEncoder).encode(newPassword);
        verify(roomRepository).save(testRoom);
    }

    @Test
    @DisplayName("Должно обновить имя комнаты.")
    void updateRoomName_ShouldUpdateNameSuccessfully() {
        String newName = "New Room Name";
        when(roomRepository.save(testRoom)).thenReturn(testRoom);

        Room result = roomService.updateRoomName(testRoom, newName);

        assertNotNull(result);
        assertEquals(newName, testRoom.getName());
        verify(roomRepository).save(testRoom);
    }

    @Test
    @DisplayName("Должно обновить описание комнаты.")
    void updateRoomDescription_ShouldUpdateDescriptionSuccessfully() {
        String newDescription = "New Description";
        when(roomRepository.save(testRoom)).thenReturn(testRoom);

        Room result = roomService.updateRoomDescription(testRoom, newDescription);

        assertNotNull(result);
        assertEquals(newDescription, testRoom.getDescription());
        verify(roomRepository).save(testRoom);
    }

    @Test
    @DisplayName("Должно удалить комнату.")
    void deleteRoom_ShouldDeleteRoomAndCleanup() {
        roomService.deleteRoom(testRoomId);

        verify(roomRepository).deleteById(testRoomId);
        verify(participantService).removeRoom(testRoomId);
        verify(videoControlService).removeRoomVideoState(testRoomId);
    }

    @Test
    @DisplayName("Должно добавить участника в комнату.")
    void addParticipant_ShouldAddParticipantAndSendNotification() {
        String username = "testUser";

        roomService.addParticipant(testRoomId, username);

        verify(participantService).addParticipant(testRoomId, username);
        verify(participantService).sendJoinLeaveNotification(testRoomId, username, true);
    }

    @Test
    @DisplayName("Должно удалить участника из комнаты.")
    void removeParticipant_WhenParticipantExists_ShouldRemoveAndSendNotification() {
        String username = "testUser";
        when(participantService.removeParticipant(testRoomId, username)).thenReturn(true);

        roomService.removeParticipant(testRoomId, username);

        verify(participantService).removeParticipant(testRoomId, username);
        verify(participantService).sendJoinLeaveNotification(testRoomId, username, false);
    }

    @Test
    @DisplayName("Не должно удалить участника из комнаты.")
    void removeParticipant_WhenParticipantDoesNotExist_ShouldNotSendNotification() {
        String username = "nonExistentUser";
        when(participantService.removeParticipant(testRoomId, username)).thenReturn(false);

        roomService.removeParticipant(testRoomId, username);

        verify(participantService).removeParticipant(testRoomId, username);
        verify(participantService, never()).sendJoinLeaveNotification(testRoomId, username, false);
    }

    @Test
    @DisplayName("Должно вернуть количество участников в комнате.")
    void getParticipantCount_ShouldReturnCorrectCount() {
        int expectedCount = 10;
        when(participantService.getParticipantCount(testRoomId)).thenReturn(expectedCount);

        int result = roomService.getParticipantCount(testRoomId);

        assertEquals(expectedCount, result);
        verify(participantService).getParticipantCount(testRoomId);
    }

    @Test
    @DisplayName("Должно вернуть true при вызове forceRemoveParticipant.")
    void forceRemoveParticipant_ShouldReturnCorrectResult() {
        String username = "testUser";
        when(participantService.forceRemoveParticipant(testRoomId, username)).thenReturn(true);

        boolean result = roomService.forceRemoveParticipant(testRoomId, username);

        assertTrue(result);
        verify(participantService).forceRemoveParticipant(testRoomId, username);
    }

    @Test
    @DisplayName("Должно вернуть список участников в комнате.")
    void getParticipants_ShouldReturnParticipantsList() {
        List<String> expectedParticipants = Arrays.asList("user1", "user2", "user3");
        when(participantService.getParticipants(testRoomId)).thenReturn(expectedParticipants);

        List<String> result = roomService.getParticipants(testRoomId);

        assertEquals(expectedParticipants, result);
        verify(participantService).getParticipants(testRoomId);
    }

    @Test
    @DisplayName("Должно вернуть текущее состояние видео.")
    void getCurrentVideoState_ShouldReturnVideoState() {
        VideoControlMessage expectedMessage = new VideoControlMessage();
        when(videoControlService.getCurrentVideoState(testRoomId)).thenReturn(Optional.of(expectedMessage));

        Optional<VideoControlMessage> result = roomService.getCurrentVideoState(testRoomId);

        assertTrue(result.isPresent());
        assertEquals(expectedMessage, result.get());
        verify(videoControlService).getCurrentVideoState(testRoomId);
    }

    @Test
    @DisplayName("Должно обновить состояние видео.")
    void updateVideoState_ShouldUpdateVideoState() {
        VideoControlMessage message = new VideoControlMessage();

        roomService.updateVideoState(testRoomId, message);

        verify(videoControlService).updateVideoState(testRoomId, message);
    }

    @Test
    @DisplayName("Должно широковещать о состоянии видео.")
    void broadcastVideoControl_ShouldBroadcastMessage() {
        VideoControlMessage message = new VideoControlMessage();
        boolean excludeInitiator = true;

        roomService.broadcastVideoControl(message, excludeInitiator);

        verify(videoControlService).broadcastVideoControl(message, excludeInitiator);
    }

    @Test
    @DisplayName("Должно вернуть URL для загрузки аватара.")
    void saveAvatar_ShouldUploadFileAndReturnUrl() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        String fileKey = "avatar-key";
        String presignedUrl = "https://example.com/avatar.jpg";

        when(minioService.uploadFile(file)).thenReturn(fileKey);
        when(minioService.getPresignedUrl(fileKey)).thenReturn(presignedUrl);

        String result = roomService.saveAvatar(file);

        assertEquals(presignedUrl, result);
        verify(minioService).uploadFile(file);
        verify(minioService).getPresignedUrl(fileKey);
    }

    @Test
    @DisplayName("Должно выбросить исключение при ошибке загрузки аватара.")
    void saveAvatar_WhenUploadFails_ShouldThrowException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(minioService.uploadFile(file)).thenThrow(new RuntimeException("Upload failed"));

        assertThrows(Exception.class, () -> roomService.saveAvatar(file));
        verify(minioService).uploadFile(file);
        verify(minioService, never()).getPresignedUrl(anyString());
    }

    @Test
    @DisplayName("Должно вернуть URL для загрузки обложки.")
    void saveCover_ShouldUploadFileAndReturnUrl() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        String fileKey = "cover-key";
        String presignedUrl = "https://example.com/cover.jpg";

        when(minioService.uploadFile(file)).thenReturn(fileKey);
        when(minioService.getPresignedUrl(fileKey)).thenReturn(presignedUrl);

        String result = roomService.saveCover(file);

        assertEquals(presignedUrl, result);
        verify(minioService).uploadFile(file);
        verify(minioService).getPresignedUrl(fileKey);
    }

    @Test
    @DisplayName("Должно выбросить исключение при ошибке загрузки обложки.")
    void updateRoom_ShouldUpdateRoomSuccessfully() {
        when(roomRepository.save(testRoom)).thenReturn(testRoom);

        Room result = roomService.updateRoom(testRoom);

        assertNotNull(result);
        assertEquals(testRoom, result);
        verify(roomRepository).save(testRoom);
    }
}