package com.luanlana.chat_server.interceptor;

import com.luanlana.chat_server.service.WebSocketRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConnectionInterceptor implements ChannelInterceptor {

    private final WebSocketRegistry sessionRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            try {
                sessionRegistry.increment();
                System.out.println("Novo cliente conectado. Conexões ativas: " + sessionRegistry.getActiveSessions());
            } catch (RuntimeException ex) {
                System.out.println("Tentativa de conexão rejeitada: " + ex.getMessage());
                throw ex;
            }
        }
        return message;
    }
}