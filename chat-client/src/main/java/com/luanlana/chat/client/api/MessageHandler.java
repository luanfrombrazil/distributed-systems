package com.luanlana.chat.client.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luanlana.chat.client.model.Message;
import lombok.Setter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.lang.reflect.Type;


public class MessageHandler implements StompFrameHandler {
    @Setter
    private MessageListener listener;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MessageHandler() {
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return byte[].class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        try {
            Message message = objectMapper.readValue((byte[]) payload, Message.class);
            listener.onMessageReceived(message);
        } catch (Exception e) {
            System.err.println("Erro ao processar mensagem recebida: " + e.getMessage());
        }
    }
}