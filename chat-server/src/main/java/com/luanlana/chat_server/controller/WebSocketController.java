package com.luanlana.chat_server.controller;

import com.luanlana.chat_server.dto.ChatMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {
    @MessageMapping("/chat.addUser/{groupId}")
    @SendTo("/groups/{groupId}")
    public ChatMessage addUser(@DestinationVariable String groupId, @Payload ChatMessage chatMessage) {
        return chatMessage;
    }
}