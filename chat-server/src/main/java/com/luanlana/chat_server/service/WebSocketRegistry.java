package com.luanlana.chat_server.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WebSocketRegistry {

    private final AtomicInteger connectionCount = new AtomicInteger(0);
    private static final int MAX_CONNECTIONS = 5;

    public void increment() {
        if (connectionCount.incrementAndGet() > MAX_CONNECTIONS) {
            connectionCount.decrementAndGet();
            throw new RuntimeException("Limite de conexões simultâneas atingido.");
        }
    }

    public void decrement() {
        connectionCount.decrementAndGet();
    }

    @EventListener
    private void handleSessionDisconnect(SessionDisconnectEvent event) {
        this.decrement();
        System.out.println("Cliente desconectado. Conexões ativas: " + connectionCount.get());
    }

    public int getActiveSessions() {
        return connectionCount.get();
    }
}