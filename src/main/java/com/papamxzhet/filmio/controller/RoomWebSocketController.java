package com.papamxzhet.filmio.controller;

import com.papamxzhet.filmio.model.ParticipantMessage;
import com.papamxzhet.filmio.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class RoomWebSocketController {

    @Autowired
    private RoomService roomService;

    @MessageMapping("/join")
    @SendTo("/topic/participants")
    public List<String> join(ParticipantMessage message) {
        String username = message.getUsername();
        roomService.addParticipant(message.getRoomId(), username);
        return roomService.getParticipants(message.getRoomId());
    }

    @MessageMapping("/leave")
    @SendTo("/topic/participants")
    public List<String> leave(ParticipantMessage message) {
        String username = message.getUsername();
        roomService.removeParticipant(message.getRoomId(), username);
        return roomService.getParticipants(message.getRoomId());
    }
}
