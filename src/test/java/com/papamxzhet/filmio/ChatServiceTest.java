package com.papamxzhet.filmio;

import com.papamxzhet.filmio.model.ChatMessage;
import com.papamxzhet.filmio.model.MessageReaction;
import com.papamxzhet.filmio.model.User;
import com.papamxzhet.filmio.repository.ChatMessageRepository;
import com.papamxzhet.filmio.repository.MessageReactionRepository;
import com.papamxzhet.filmio.repository.UserRepository;
import com.papamxzhet.filmio.service.ChatService;
import com.papamxzhet.filmio.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService Tests")
class ChatServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private MessageReactionRepository messageReactionRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatService chatService;

    @Captor
    private ArgumentCaptor<ChatMessage> messageCaptor;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Captor
    private ArgumentCaptor<Object> objectCaptor;

    private ChatMessage testMessage;
    private User testUser;
    private String testRoomId;
    private String testUsername;
    private String originalContent;
    private String encryptedContent;
    private Long testMessageId;

    @BeforeEach
    void setUp() {
        testRoomId = "028f64f4-6ef8-4d29-8457-124c98f3090b";
        testUsername = "testUser";
        testMessageId = 1L;
        originalContent = "Hello World!";
        encryptedContent = "encryptedHelloWorld";

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(testUsername);

        testMessage = new ChatMessage();
        testMessage.setId(testMessageId);
        testMessage.setRoomId(testRoomId);
        testMessage.setUsername(testUsername);
        testMessage.setEncryptedContent(originalContent);
        testMessage.setTimestamp(LocalDateTime.now());
    }

    @Test
    @DisplayName("Сообщение должно быть успешно отправлено без родителя(без ответа)")
    void sendMessage_WithoutParent_ShouldSendSuccessfully() {
        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setId(testMessageId);
        savedMessage.setRoomId(testRoomId);
        savedMessage.setUsername(testUsername);
        savedMessage.setEncryptedContent(encryptedContent);
        savedMessage.setTimestamp(LocalDateTime.now());

        when(encryptionService.encrypt(originalContent)).thenReturn(encryptedContent);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        ChatMessage result = chatService.sendMessage(testRoomId, testMessage);

        assertNotNull(result);
        assertEquals(testMessageId, result.getId());
        assertEquals(originalContent, result.getEncryptedContent());

        verify(encryptionService).encrypt(originalContent);
        verify(chatMessageRepository).save(messageCaptor.capture());
        verify(messagingTemplate).convertAndSend(eq("/topic/" + testRoomId), any(ChatMessage.class));

        ChatMessage capturedMessage = messageCaptor.getValue();
        assertEquals(testRoomId, capturedMessage.getRoomId());
        assertEquals(encryptedContent, capturedMessage.getEncryptedContent());
        assertNotNull(capturedMessage.getTimestamp());
    }

    @Test
    @DisplayName("Сообщение должно быть успешно отправлено вместе с родительским сообщением (с ответом)")
    void sendMessage_WithParentMessage_ShouldSendSuccessfully() {
        Long parentId = 2L;
        ChatMessage parentMessage = new ChatMessage();
        parentMessage.setId(parentId);
        parentMessage.setEncryptedContent("parentEncryptedContent");

        ChatMessage messageWithParent = new ChatMessage();
        messageWithParent.setEncryptedContent(originalContent);
        messageWithParent.setUsername(testUsername);

        ChatMessage parentRef = new ChatMessage();
        parentRef.setId(parentId);
        messageWithParent.setParentMessage(parentRef);

        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setId(testMessageId);
        savedMessage.setParentMessage(parentMessage);
        savedMessage.setRoomId(testRoomId);
        savedMessage.setEncryptedContent(encryptedContent);
        savedMessage.setTimestamp(LocalDateTime.now());

        when(encryptionService.encrypt(originalContent)).thenReturn(encryptedContent);
        when(encryptionService.decrypt("parentEncryptedContent")).thenReturn("Decrypted parent content");
        when(chatMessageRepository.findById(parentId)).thenReturn(Optional.of(parentMessage));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        ChatMessage result = chatService.sendMessage(testRoomId, messageWithParent);

        assertNotNull(result);
        verify(chatMessageRepository).findById(parentId);
        verify(encryptionService).decrypt("parentEncryptedContent");
        verify(messagingTemplate).convertAndSend(eq("/topic/" + testRoomId), any(ChatMessage.class));
    }

    @Test
    @DisplayName("Должно получать сообщения с расшифровкой")
    void getMessages_ShouldReturnDecryptedMessages() {
        ChatMessage message1 = new ChatMessage();
        message1.setId(1L);
        message1.setEncryptedContent("encrypted1");

        ChatMessage message2 = new ChatMessage();
        message2.setId(2L);
        message2.setEncryptedContent("encrypted2");

        List<ChatMessage> encryptedMessages = Arrays.asList(message1, message2);
        Page<ChatMessage> messagesPage = new PageImpl<>(encryptedMessages);

        when(chatMessageRepository.findByRoomIdWithReactions(eq(testRoomId), any(Pageable.class)))
                .thenReturn(messagesPage);
        when(encryptionService.decrypt("encrypted1")).thenReturn("decrypted1");
        when(encryptionService.decrypt("encrypted2")).thenReturn("decrypted2");

        List<ChatMessage> result = chatService.getMessages(testRoomId, 0, 10);

        assertEquals(2, result.size());
        assertEquals("decrypted1", result.get(0).getEncryptedContent());
        assertEquals("decrypted2", result.get(1).getEncryptedContent());

        verify(encryptionService, times(2)).decrypt(anyString());
        verify(chatMessageRepository).findByRoomIdWithReactions(eq(testRoomId), any(Pageable.class));
    }

    @Test
    @DisplayName("Должен обрабатывать ошибки дешифрования в getMessages")
    void getMessages_WithDecryptionError_ShouldHandleGracefully() {
        ChatMessage message = new ChatMessage();
        message.setId(1L);
        message.setEncryptedContent("corruptedEncryption");

        List<ChatMessage> messages = Arrays.asList(message);
        Page<ChatMessage> messagesPage = new PageImpl<>(messages);

        when(chatMessageRepository.findByRoomIdWithReactions(eq(testRoomId), any(Pageable.class)))
                .thenReturn(messagesPage);
        when(encryptionService.decrypt("corruptedEncryption"))
                .thenThrow(new RuntimeException("Decryption failed"));

        List<ChatMessage> result = chatService.getMessages(testRoomId, 0, 10);

        assertEquals(1, result.size());
        assertEquals("Ошибка: Невозможно расшифровать сообщение", result.get(0).getEncryptedContent());
    }

    @Test
    @DisplayName("Сообщение должно быть успешно удалено")
    void deleteMessage_WhenMessageExists_ShouldDeleteSuccessfully() {
        ChatMessage messageToDelete = new ChatMessage();
        messageToDelete.setId(testMessageId);
        messageToDelete.setRoomId(testRoomId);

        when(chatMessageRepository.findById(testMessageId)).thenReturn(Optional.of(messageToDelete));

        chatService.deleteMessage(testMessageId);

        verify(chatMessageRepository).deleteById(testMessageId);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/" + testRoomId + "/deletions"),
                objectCaptor.capture()
        );

        Map<String, Object> deletionEvent = (Map<String, Object>) objectCaptor.getValue();
        assertEquals("DELETE_MESSAGE", deletionEvent.get("type"));
        assertEquals(testMessageId, deletionEvent.get("messageId"));
    }

    @Test
    @DisplayName("Не должно удалять сообщение, если оно не существует.")
    void deleteMessage_WhenMessageDoesNotExist_ShouldNotDelete() {
        when(chatMessageRepository.findById(testMessageId)).thenReturn(Optional.empty());

        chatService.deleteMessage(testMessageId);

        verify(chatMessageRepository, never()).deleteById(anyLong());
        verify(messagingTemplate, never()).convertAndSend(anyString());
    }

    @Test
    @DisplayName("Должен успешно добавить реакцию")
    void toggleReaction_AddNewReaction_ShouldAddSuccessfully() {
        String emoji = "👍";
        ChatMessage message = new ChatMessage();
        message.setId(testMessageId);
        message.setRoomId(testRoomId);

        MessageReaction savedReaction = new MessageReaction();
        savedReaction.setId(10L);
        savedReaction.setEmoji(emoji);
        savedReaction.setUser(testUser);

        when(chatMessageRepository.findById(testMessageId)).thenReturn(Optional.of(message));
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
        when(messageReactionRepository.findByMessageIdAndUser_IdAndEmoji(testMessageId, testUser.getId(), emoji))
                .thenReturn(Optional.empty());
        when(messageReactionRepository.save(any(MessageReaction.class))).thenReturn(savedReaction);

        Map<String, Object> result = chatService.toggleReaction(testMessageId, testUsername, emoji);

        assertEquals("ADD_REACTION", result.get("type"));
        assertEquals(testMessageId, result.get("messageId"));
        assertEquals(testUsername, result.get("username"));
        assertEquals(emoji, result.get("emoji"));
        assertEquals(10L, result.get("reactionId"));

        verify(messageReactionRepository).save(any(MessageReaction.class));
        verify(messagingTemplate).convertAndSend(
                eq("/topic/" + testRoomId + "/reactions"),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("Должен удалить существующую реакцию")
    void toggleReaction_RemoveExistingReaction_ShouldRemoveSuccessfully() {
        String emoji = "👍";
        ChatMessage message = new ChatMessage();
        message.setId(testMessageId);
        message.setRoomId(testRoomId);

        MessageReaction existingReaction = new MessageReaction();
        existingReaction.setId(10L);
        existingReaction.setEmoji(emoji);
        existingReaction.setUser(testUser);

        when(chatMessageRepository.findById(testMessageId)).thenReturn(Optional.of(message));
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
        when(messageReactionRepository.findByMessageIdAndUser_IdAndEmoji(testMessageId, testUser.getId(), emoji))
                .thenReturn(Optional.of(existingReaction));

        Map<String, Object> result = chatService.toggleReaction(testMessageId, testUsername, emoji);

        assertEquals("REMOVE_REACTION", result.get("type"));
        assertEquals(testMessageId, result.get("messageId"));
        assertEquals(testUsername, result.get("username"));
        assertEquals(emoji, result.get("emoji"));
        assertNull(result.get("reactionId"));

        verify(messageReactionRepository).delete(existingReaction);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/" + testRoomId + "/reactions"),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("Должен выдавать исключение, если сообщение для реакции не найдено")
    void toggleReaction_MessageNotFound_ShouldThrowException() {
        when(chatMessageRepository.findById(testMessageId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> chatService.toggleReaction(testMessageId, testUsername, "👍"));

        assertEquals("Сообщение не найдено", exception.getMessage());
        verify(messageReactionRepository, never()).save(any());
        verify(messageReactionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Должен выдавать исключение, если пользователь не найден для реакции")
    void toggleReaction_UserNotFound_ShouldThrowException() {
        ChatMessage message = new ChatMessage();
        message.setId(testMessageId);

        when(chatMessageRepository.findById(testMessageId)).thenReturn(Optional.of(message));
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> chatService.toggleReaction(testMessageId, testUsername, "👍"));

        assertEquals("Пользователь не найден", exception.getMessage());
    }

    @Test
    @DisplayName("Должен получить реакции на сообщение")
    void getReactions_ShouldReturnReactionsList() {
        List<MessageReaction> expectedReactions = Arrays.asList(
                new MessageReaction(),
                new MessageReaction()
        );

        when(messageReactionRepository.findByMessageId(testMessageId)).thenReturn(expectedReactions);

        List<MessageReaction> result = chatService.getReactions(testMessageId);

        assertEquals(expectedReactions, result);
        verify(messageReactionRepository).findByMessageId(testMessageId);
    }

    @Test
    @DisplayName("Должен правильно обрабатывать отправку сообщений WebSocket.")
    void sendMessage_ShouldSendCorrectWebSocketMessage() {
        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setId(testMessageId);
        savedMessage.setRoomId(testRoomId);
        savedMessage.setUsername(testUsername);
        savedMessage.setEncryptedContent(encryptedContent);
        savedMessage.setTimestamp(LocalDateTime.now());

        when(encryptionService.encrypt(originalContent)).thenReturn(encryptedContent);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        chatService.sendMessage(testRoomId, testMessage);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/" + testRoomId),
                objectCaptor.capture()
        );

        ChatMessage webSocketMessage = (ChatMessage) objectCaptor.getValue();
        assertEquals(testMessageId, webSocketMessage.getId());
        assertEquals(testRoomId, webSocketMessage.getRoomId());
        assertEquals(testUsername, webSocketMessage.getUsername());
        assertEquals(originalContent, webSocketMessage.getEncryptedContent());
    }

    @Test
    @DisplayName("Должны правильно обрабатывать параметры пагинации")
    void getMessages_WithPagination_ShouldUseCorrectParameters() {
        int page = 2;
        int size = 20;

        when(chatMessageRepository.findByRoomIdWithReactions(eq(testRoomId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        chatService.getMessages(testRoomId, page, size);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(chatMessageRepository).findByRoomIdWithReactions(eq(testRoomId), pageableCaptor.capture());

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(page, capturedPageable.getPageNumber());
        assertEquals(size, capturedPageable.getPageSize());
        assertEquals("timestamp: ASC", capturedPageable.getSort().toString());
    }

    @Test
    @DisplayName("Должен правильно обрабатывать отправку сообщений WebSocket с родительским сообщением.")
    void sendMessage_WithParentDecryptionError_ShouldHandleGracefully() {
        ChatMessage parentMessage = new ChatMessage();
        parentMessage.setId(2L);
        parentMessage.setEncryptedContent("corruptedParentContent");

        ChatMessage messageWithParent = new ChatMessage();
        messageWithParent.setEncryptedContent(originalContent);
        messageWithParent.setUsername(testUsername);

        ChatMessage parentRef = new ChatMessage();
        parentRef.setId(2L);
        messageWithParent.setParentMessage(parentRef);

        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setId(testMessageId);
        savedMessage.setParentMessage(parentMessage);
        savedMessage.setRoomId(testRoomId);
        savedMessage.setEncryptedContent(encryptedContent);
        savedMessage.setTimestamp(LocalDateTime.now());

        when(encryptionService.encrypt(originalContent)).thenReturn(encryptedContent);
        when(encryptionService.decrypt("corruptedParentContent"))
                .thenThrow(new RuntimeException("Decryption failed"));
        when(chatMessageRepository.findById(2L)).thenReturn(Optional.of(parentMessage));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        ChatMessage result = chatService.sendMessage(testRoomId, messageWithParent);

        assertNotNull(result);
        verify(messagingTemplate).convertAndSend(eq("/topic/" + testRoomId), objectCaptor.capture());

        ChatMessage webSocketMessage = (ChatMessage) objectCaptor.getValue();
        assertNotNull(webSocketMessage.getParentMessage());
        assertEquals("Ошибка: Невозможно расшифровать сообщение",
                webSocketMessage.getParentMessage().getEncryptedContent());
    }

    @Test
    @DisplayName("Должно создать событие реакции с правильной структурой")
    void toggleReaction_ShouldCreateCorrectReactionEvent() {
        String emoji = "❤️";
        ChatMessage message = new ChatMessage();
        message.setId(testMessageId);
        message.setRoomId(testRoomId);

        when(chatMessageRepository.findById(testMessageId)).thenReturn(Optional.of(message));
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
        when(messageReactionRepository.findByMessageIdAndUser_IdAndEmoji(testMessageId, testUser.getId(), emoji))
                .thenReturn(Optional.empty());
        when(messageReactionRepository.save(any(MessageReaction.class))).thenAnswer(invocation -> {
            MessageReaction reaction = invocation.getArgument(0);
            reaction.setId(99L);
            return reaction;
        });

        Map<String, Object> result = chatService.toggleReaction(testMessageId, testUsername, emoji);

        assertTrue(result.containsKey("type"));
        assertTrue(result.containsKey("messageId"));
        assertTrue(result.containsKey("username"));
        assertTrue(result.containsKey("emoji"));
        assertTrue(result.containsKey("reactionId"));

        assertEquals("ADD_REACTION", result.get("type"));
        assertEquals(testMessageId, result.get("messageId"));
        assertEquals(testUsername, result.get("username"));
        assertEquals(emoji, result.get("emoji"));
        assertEquals(99L, result.get("reactionId"));
    }
}