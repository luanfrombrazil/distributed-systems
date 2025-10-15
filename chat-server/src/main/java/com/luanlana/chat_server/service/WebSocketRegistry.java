package com.luanlana.chat_server.service;

import com.luanlana.chat_server.log.LogWriter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WebSocketRegistry {
    private LogWriter logger;

    {
        try {
            logger = new LogWriter("logs/serverconnection_log.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final AtomicInteger connectionCount = new AtomicInteger(0);
    private static final int MAX_CONNECTIONS = 5;

    public void increment() {
        if (connectionCount.incrementAndGet() > MAX_CONNECTIONS) {
            throw new RuntimeException("LIMITE DE CONEXÕES ATINGIDAS, CLIENTE AVISADO");
        }
    }

    public void decrement() {
        connectionCount.decrementAndGet();
    }

    @EventListener
    private void handleSessionDisconnect(SessionDisconnectEvent event) {
        this.decrement();
        logger.print("CLIENTE DESCONECTADO, ATIVOS: " + connectionCount.get());
    }

    public int getActiveSessions() {
        return connectionCount.get();
    }
}