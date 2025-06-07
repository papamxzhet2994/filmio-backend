package com.papamxzhet.filmio.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Controller
public class NetworkLatencyController {

    private final SimpMessagingTemplate messagingTemplate;

    public NetworkLatencyController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/ping/{roomId}")
    public void handlePing(@Payload Map<String, Object> pingMessage) {
        String username = (String) pingMessage.get("username");
        Long clientTimestamp = Long.valueOf(pingMessage.get("timestamp").toString());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", clientTimestamp);

        System.out.println("PING from " + username + ", echoing timestamp: " + clientTimestamp);

        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/pong",
                response
        );
    }
}