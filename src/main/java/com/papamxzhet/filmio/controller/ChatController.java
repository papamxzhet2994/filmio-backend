package com.papamxzhet.filmio.controller;

import com.papamxzhet.filmio.model.ChatMessage;
import com.papamxzhet.filmio.repository.ChatMessageRepository;
import com.papamxzhet.filmio.service.EncryptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/{roomId}")
    public ChatMessage sendMessage(@PathVariable String roomId, @RequestBody ChatMessage message) {
        System.out.println("Received message: " + message); // Лог входящего сообщения

        message.setRoomId(roomId);
        message.setTimestamp(LocalDateTime.now());
        message.setEncryptedContent(encryptionService.encrypt(message.getEncryptedContent()));

        if (message.getParentMessage() != null && message.getParentMessage().getId() != null) {
            Optional<ChatMessage> parent = chatMessageRepository.findById(message.getParentMessage().getId());
            parent.ifPresent(message::setParentMessage);
        }

        ChatMessage savedMessage = chatMessageRepository.save(message);
        System.out.println("Saved message: " + savedMessage); // Лог сохраненного сообщения

        // Расшифровка перед отправкой через WebSocket
        savedMessage.setEncryptedContent(encryptionService.decrypt(savedMessage.getEncryptedContent()));
        messagingTemplate.convertAndSend("/topic/" + roomId, savedMessage);

        return savedMessage;
    }


    @GetMapping("/{roomId}")
    public List<ChatMessage> getMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "timestamp"));
        Page<ChatMessage> messagesPage = chatMessageRepository.findByRoomId(roomId, pageable);

        List<ChatMessage> messages = messagesPage.getContent();

        // Лог расшифровки сообщений
        messages.forEach(message -> {
            try {
                message.setEncryptedContent(encryptionService.decrypt(message.getEncryptedContent()));
            } catch (Exception e) {
                System.err.println("Failed to decrypt message: " + message.getId());
                message.setEncryptedContent("Error: Unable to decrypt message");
            }
        });

        System.out.println("Fetched messages for room " + roomId + ": " + messages);
        return messages;
    }


    @DeleteMapping("/{messageId}")
    public void deleteMessage(@PathVariable Long messageId) {
        chatMessageRepository.deleteById(messageId);
    }
}
