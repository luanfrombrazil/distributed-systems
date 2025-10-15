package com.luanlana.chat.client.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luanlana.chat.client.dto.CreateGroupRequest;
import com.luanlana.chat.client.model.Group;
import com.luanlana.chat.client.model.Message;
import com.luanlana.chat.client.dto.MessageRequest;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChatApiService {

    private final HttpClient httpClient;
    private Random random = new Random();
    private final int MAX_TRIES = 100;
    private final ObjectMapper objectMapper;
    private final StompSessionManager stompManager;
    private final MessageHandler messageHandler;
    @Getter
    @Setter
    private Instant lastSyncTimestamp;
    @Setter
    private MessageListener listener;

    public ChatApiService(StompSessionManager stompManager, MessageHandler messageHandler) {
        this.stompManager = stompManager;
        this.messageHandler = messageHandler;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void sendMessage(Long groupId, MessageRequest messageRequest) throws Exception {
        if (!stompManager.isConnected()) {
            stompManager.connect(groupId, messageHandler);
            List<Message> history = this.fetchMessages(groupId, lastSyncTimestamp);
            listener.onHistoryReceived(history);
        }

        String serverUrl = "http://localhost:8081/groups/" + groupId + "/messages";
        System.out.println("" + groupId + messageRequest);
        String jsonBody = objectMapper.writeValueAsString(messageRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        stompManager.resetTimer();
        long baseDelayMs = 500;
        for (int attempt = 1; attempt <= MAX_TRIES; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return;
                }
            } catch (Exception e) {
                if (attempt == MAX_TRIES) {
                    throw new RuntimeException("VERIFIQUE SE O SERVIDOR ESTÁ ONLINE: ", e);
                }
                long backoffDelay = baseDelayMs * (long) Math.pow(1.5, attempt - 1);
                long jitter = random.nextInt(1000);
                long delay = backoffDelay + jitter;

                Thread.sleep(delay);
            }
        }
    }

    public List<Message> fetchMessages(Long groupId, Instant timestamp) throws Exception {
        String sinceParameter = timestamp + "";

        /*
        DEIXEI FIXO PARA BUSCAR APENAS AS 15 MENSAGENS ANTERIORES
         */
        String serverUrl = String.format(
                "http://localhost:8081/groups/%d/messages?since=%s&limit=%d",
                groupId,
                sinceParameter,
                15
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl))
                .GET()
                .build();

        long baseDelayMs = 500;
        for (int attempt = 1; attempt <= MAX_TRIES; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {

                    String jsonBody = response.body();
                    List<Message> mensagens = objectMapper.readValue(jsonBody, new TypeReference<>() {
                    });
                    return mensagens;
                } else {
                    throw new RuntimeException("FFALHA AO BUSCAR HISTÓRICO DE MENSAGENS " + response.statusCode());
                }
            } catch (Exception e) {
                if (attempt == MAX_TRIES) {
                    throw new RuntimeException("VERIFIQUE SE O SERVIDOR ESTÁ ONLINE", e);
                }

                long backoffDelay = baseDelayMs * (long) Math.pow(2, attempt - 1);
                long jitter = random.nextInt(1000);
                long delay = backoffDelay + jitter;

                Thread.sleep(delay);
            }
        }
        throw new RuntimeException("FALHA AO BUSCAR HISTÓRICO DE MENSAGENS");
    }

    public ArrayList<Group> fetchGroups() throws Exception {
        String serverUrl = "http://localhost:8081/groups";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl))
                .GET()
                .build();

        long baseDelayMs = 500;
        for (int attempt = 1; attempt <= MAX_TRIES; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String jsonBody = response.body();
                    ArrayList<Group> groups = objectMapper.readValue(jsonBody, new TypeReference<>() {
                    });
                    return groups;
                } else {
                    throw new RuntimeException("FALHA AO BUSCAR GRUPOS: " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("TENTATIVA " + attempt + " FALHOU: " + e.getMessage());

                if (attempt == MAX_TRIES) {
                    throw new RuntimeException("FALHA APOS " + MAX_TRIES + " TANTATIVAS.", e);
                }

                long backoffDelay = baseDelayMs * (long) Math.pow(2, attempt - 1);
                long jitter = random.nextInt(1000);
                long delay = backoffDelay + jitter;

                Thread.sleep(delay);
            }
        }
        throw new RuntimeException("Falha ao buscar grupos.");
    }

    public Group newGroup(String roomName) throws Exception {
        String serverUrl = "http://localhost:8081/groups";
        CreateGroupRequest groupRequestData = new CreateGroupRequest(roomName);

        String jsonBody = objectMapper.writeValueAsString(groupRequestData);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {

            String jsonResponseBody = response.body();

            Group group = objectMapper.readValue(jsonResponseBody, new TypeReference<>() {
            });

            return group;

        } else {
            throw new RuntimeException("FALHA AO BUSCAR GRUPOS: " + response.statusCode());
        }
    }
}