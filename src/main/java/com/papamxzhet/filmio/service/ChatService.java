package com.papamxzhet.filmio.service;

import com.papamxzhet.filmio.model.ChatMessage;
import com.papamxzhet.filmio.model.MessageReaction;
import com.papamxzhet.filmio.model.User;
import com.papamxzhet.filmio.repository.ChatMessageRepository;
import com.papamxzhet.filmio.repository.MessageReactionRepository;
import com.papamxzhet.filmio.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ChatService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private MessageReactionRepository messageReactionRepository;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public ChatMessage sendMessage(String roomId, ChatMessage message) {
        System.out.println("Полученное сообщение: " + message);

        String originalContent = message.getEncryptedContent();

        message.setRoomId(roomId);
        message.setTimestamp(LocalDateTime.now());

        message.setEncryptedContent(encryptionService.encrypt(message.getEncryptedContent()));

        if (message.getParentMessage() != null && message.getParentMessage().getId() != null) {
            Optional<ChatMessage> parent = chatMessageRepository.findById(message.getParentMessage().getId());
            parent.ifPresent(message::setParentMessage);
        }

        ChatMessage savedMessage = chatMessageRepository.save(message);

        sendMessageToWebSocket(savedMessage, originalContent);

        savedMessage.setEncryptedContent(originalContent);
        return savedMessage;
    }

    public List<ChatMessage> getMessages(String roomId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "timestamp"));
        Page<ChatMessage> messagesPage = chatMessageRepository.findByRoomIdWithReactions(roomId, pageable);
        List<ChatMessage> messages = messagesPage.getContent();

        messages.forEach(message -> {
            try {
                message.setEncryptedContent(encryptionService.decrypt(message.getEncryptedContent()));
            } catch (Exception e) {
                message.setEncryptedContent("Ошибка: Невозможно расшифровать сообщение");
            }
        });

        return messages;
    }

    public void deleteMessage(Long messageId) {
        Optional<ChatMessage> messageToDelete = chatMessageRepository.findById(messageId);

        if (messageToDelete.isPresent()) {
            String roomId = messageToDelete.get().getRoomId();

            chatMessageRepository.deleteById(messageId);

            sendDeletionNotification(roomId, messageId);
        }
    }

    public Map<String, Object> toggleReaction(Long messageId, String username, String emoji) {
        Optional<ChatMessage> messageOpt = chatMessageRepository.findById(messageId);
        if (messageOpt.isEmpty()) {
            throw new RuntimeException("Сообщение не найдено");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        ChatMessage message = messageOpt.get();
        String roomId = message.getRoomId();

        Optional<MessageReaction> existingReaction = messageReactionRepository
                .findByMessageIdAndUser_IdAndEmoji(messageId, user.getId(), emoji);

        if (existingReaction.isPresent()) {
            messageReactionRepository.delete(existingReaction.get());

            Map<String, Object> reactionEvent = createReactionEvent("REMOVE_REACTION",
                    messageId, username, emoji, null);

            sendReactionNotification(roomId, reactionEvent);
            return reactionEvent;
        } else {
            MessageReaction reaction = new MessageReaction();
            reaction.setMessage(message);
            reaction.setUser(user);
            reaction.setEmoji(emoji);
            reaction.setUsername(user.getUsername());

            MessageReaction savedReaction = messageReactionRepository.save(reaction);

            Map<String, Object> reactionEvent = createReactionEvent("ADD_REACTION",
                    messageId, user.getUsername(), emoji, savedReaction.getId());

            sendReactionNotification(roomId, reactionEvent);
            return reactionEvent;
        }
    }

    public List<MessageReaction> getReactions(Long messageId) {
        return messageReactionRepository.findByMessageId(messageId);
    }

    private void sendMessageToWebSocket(ChatMessage savedMessage, String originalContent) {
        ChatMessage messageForWebSocket = new ChatMessage();
        messageForWebSocket.setId(savedMessage.getId());
        messageForWebSocket.setRoomId(savedMessage.getRoomId());
        messageForWebSocket.setUsername(savedMessage.getUsername());
        messageForWebSocket.setTimestamp(savedMessage.getTimestamp());
        messageForWebSocket.setEncryptedContent(originalContent);

        if (savedMessage.getParentMessage() != null) {
            ChatMessage parentForWebSocket = new ChatMessage();
            parentForWebSocket.setId(savedMessage.getParentMessage().getId());

            String parentContent = savedMessage.getParentMessage().getEncryptedContent();
            System.out.println("Родительский контент: " + parentContent);
            try {
                parentContent = encryptionService.decrypt(parentContent);
            } catch (Exception e) {
                System.err.println("Не удалось расшифровать родительское сообщение.: " + e.getMessage());
                parentContent = "Ошибка: Невозможно расшифровать сообщение";
            }

            parentForWebSocket.setEncryptedContent(parentContent);
            messageForWebSocket.setParentMessage(parentForWebSocket);
        }

        messagingTemplate.convertAndSend("/topic/" + savedMessage.getRoomId(), messageForWebSocket);
    }

    private void sendDeletionNotification(String roomId, Long messageId) {
        Map<String, Object> deletionEvent = new HashMap<>();
        deletionEvent.put("type", "DELETE_MESSAGE");
        deletionEvent.put("messageId", messageId);

        messagingTemplate.convertAndSend("/topic/" + roomId + "/deletions", deletionEvent);
    }

    private void sendReactionNotification(String roomId, Map<String, Object> reactionEvent) {
        messagingTemplate.convertAndSend("/topic/" + roomId + "/reactions", reactionEvent);
    }

    private Map<String, Object> createReactionEvent(String type, Long messageId,
                                                    String username, String emoji, Long reactionId) {
        Map<String, Object> reactionEvent = new HashMap<>();
        reactionEvent.put("type", type);
        reactionEvent.put("messageId", messageId);
        reactionEvent.put("username", username);
        reactionEvent.put("emoji", emoji);

        if (reactionId != null) {
            reactionEvent.put("reactionId", reactionId);
        }

        return reactionEvent;
    }
}