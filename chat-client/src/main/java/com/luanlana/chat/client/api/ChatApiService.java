package com.luanlana.chat.client.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luanlana.chat.client.model.CreateGroupRequest;
import com.luanlana.chat.client.model.Group;
import com.luanlana.chat.client.model.Message;
import com.luanlana.chat.client.model.MessageRequest;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class ChatApiService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final StompSessionManager stompManager;
    private final MessageHandler messageHandler;

    public ChatApiService(StompSessionManager stompManager, MessageHandler messageHandler) {
        this.stompManager = stompManager;
        this.messageHandler = messageHandler;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void sendMessage(Long groupId, MessageRequest messageRequest) throws Exception {
        String serverUrl = "http://localhost:8081/groups/" + groupId + "/messages";
        System.out.println(""+groupId + messageRequest);
        String jsonBody = objectMapper.writeValueAsString(messageRequest);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {

            String jsonResponseBody = response.body();
            System.out.println("JSON recebido do servidor: " + jsonResponseBody);
        }
        try {
            if (!stompManager.isConnected()) {
                System.out.println("Conexão WebSocket inativa. Tentando reconectar...");
                stompManager.connect(groupId, messageHandler);
            }

            System.out.println("Enviando mensagem via HTTP POST...");

        } catch (Exception e) {
            System.err.println("Falha ao enviar mensagem ou conectar ao WebSocket: " + e.getMessage());
        }
    }

    public List<Message> fetchMessages(Long groupId) throws Exception{
        String sinceParameter = Instant.now().minus(15, ChronoUnit.MINUTES)+"";

        String serverUrl = String.format(
                "http://localhost:8081/groups/%d/messages?since=%s&limit=%d",
                groupId,
                sinceParameter,
                15
        );

        System.out.println("Montando requisição GET para: " + serverUrl);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {

            String jsonBody = response.body();
            System.out.println("JSON recebido do servidor: " + jsonBody);

            List<Message> mensagens = objectMapper.readValue(jsonBody, new TypeReference<>() {
            });

            return mensagens;

        } else {
            throw new RuntimeException("Falha ao buscar histórico de mensagens. Status: " + response.statusCode());
        }
    }

    public ArrayList<Group> fetchGroups() throws Exception {
        String serverUrl = "http://localhost:8081/groups";
        System.out.println("Montando requisição GET para: " + serverUrl);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {

            String jsonBody = response.body();
            System.out.println("JSON recebido do servidor: " + jsonBody);

            ArrayList<Group> groups = objectMapper.readValue(jsonBody, new TypeReference<>() {
            });

            return groups;

        } else {
            throw new RuntimeException("Falha ao buscar grupos. Status: " + response.statusCode());
        }
    }

    public Group newGroup(String roomName) throws Exception {
        String serverUrl = "http://localhost:8081/groups";
        System.out.println("Montando requisição POST para: " + serverUrl);
        CreateGroupRequest groupRequestData = new CreateGroupRequest(roomName);

        String jsonBody = objectMapper.writeValueAsString(groupRequestData);

        System.out.println("Corpo JSON que será enviado: " + jsonBody);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {

            String jsonResponseBody = response.body();
            System.out.println("JSON recebido do servidor: " + jsonResponseBody);

            Group group = objectMapper.readValue(jsonResponseBody, new TypeReference<>() {
            });

            return group;

        } else {
            throw new RuntimeException("Falha ao buscar grupos. Status: " + response.statusCode());
        }
    }


}