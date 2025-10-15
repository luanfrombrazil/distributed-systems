package com.luanlana.chat_server.interceptor;

import com.luanlana.chat_server.log.LogWriter;
import com.luanlana.chat_server.service.WebSocketRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ConnectionInterceptor implements ChannelInterceptor {
    private LogWriter logger;
    {
        try {
            logger = new LogWriter("logs/serverconnection_log.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private final WebSocketRegistry sessionRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            try {
                sessionRegistry.increment();
                logger.print("CLIENTE CONECTADO, ATIVOS: " + sessionRegistry.getActiveSessions());
            } catch (RuntimeException ex) {
                logger.print("ERRO DE CONEXÃO, " + ex.getMessage());
                throw ex;
            }
        }
        return message;
    }
}