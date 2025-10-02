package com.luanlana.chat.client.api;

import com.luanlana.chat.client.model.Message;
import com.luanlana.chat.client.ui.ChatWindowController;
import lombok.Setter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class StompSessionManager {

    private final String serverUrl = "ws://localhost:8081/ws-chat";
    private StompSession stompSession;
    @Setter
    private ChatApiService chatApiService;
    @Setter
    private ChatWindowController windowController;


    public void connect(Long groupId, MessageHandler messageHandler) throws ExecutionException, InterruptedException {
        if (isConnected()) {
            System.out.println("Já está conectado.");
            return;
        }

        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        this.stompSession = stompClient.connectAsync(serverUrl, new StompSessionHandlerAdapter() {}).get();
        System.out.println("Conexão WebSocket estabelecida. SessionId: " + stompSession.getSessionId());

        subscribeToGroupTopic(groupId, messageHandler);
    }

    private void subscribeToGroupTopic(Long groupId, MessageHandler messageHandler) {
        String topic = "/groups/" + groupId;
        stompSession.subscribe(topic, messageHandler);
        System.out.println("Inscrito no tópico: " + topic);
    }

    public boolean isConnected() {
        return this.stompSession != null && this.stompSession.isConnected();
    }

    public void disconnect() {
        if (isConnected()) {
            stompSession.disconnect();
            System.out.println("Desconectado.");
        }
    }
}