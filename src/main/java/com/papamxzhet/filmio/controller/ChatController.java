package com.papamxzhet.filmio.controller;

import com.papamxzhet.filmio.model.ChatMessage;
import com.papamxzhet.filmio.model.MessageReaction;
import com.papamxzhet.filmio.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/{roomId}")
    public ResponseEntity<ChatMessage> sendMessage(@PathVariable String roomId,
                                                   @RequestBody ChatMessage message) {
        try {
            ChatMessage savedMessage = chatService.sendMessage(roomId, message);
            return ResponseEntity.ok(savedMessage);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<List<ChatMessage>> getMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<ChatMessage> messages = chatService.getMessages(roomId, page, size);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId) {
        try {
            chatService.deleteMessage(messageId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{messageId}/reactions")
    public ResponseEntity<Map<String, Object>> addReaction(
            @PathVariable Long messageId,
            @RequestParam String username,
            @RequestParam String emoji) {
        try {
            Map<String, Object> result = chatService.toggleReaction(messageId, username, emoji);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            if (e.getMessage().equals("Сообщение не найдено")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{messageId}/reactions")
    public ResponseEntity<List<MessageReaction>> getReactions(@PathVariable Long messageId) {
        try {
            List<MessageReaction> reactions = chatService.getReactions(messageId);
            return ResponseEntity.ok(reactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}