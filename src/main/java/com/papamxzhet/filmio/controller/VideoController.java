package com.papamxzhet.filmio.controller;

import com.papamxzhet.filmio.model.VideoControlMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class VideoController {

    private final SimpMessagingTemplate messagingTemplate;

    public VideoController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/video/{roomId}")
    public void handleVideoControl(VideoControlMessage message) {
        System.out.println("Получено WebSocket-сообщение: " + message);

        messagingTemplate.convertAndSend("/topic/video/" + message.getRoomId(), message);
    }
}
