package com.luanlana.chat.client;

import com.luanlana.chat.client.api.ChatApiService;
import com.luanlana.chat.client.api.MessageHandler;
import com.luanlana.chat.client.api.StompSessionManager;
import com.luanlana.chat.client.view.ChatWindowController;

import javax.swing.*;

/*
* CHAT CLIENTE-SERVIDOR
* SISTEMAS DISTRIBUIDOS
* LUAN LANA MAIA.
*
* MINHA IDEIA FOI APROVEITAR O CONTEUDO QUE APRENDI NA DISCIPLINA DE TEES DESENVOLVIMENTO BASEADO EM FRAMEWORKS, ONDE
* COMEÇAMOS A VER SOBRE SPRING E IMPLEMENTAR MEU SERVIDOR A PARTIR DISSO.
*
* A IDEIA INICIAL ERA IMPLEMENTAR API REST SEM MANTER CONEXÃO ALGUMA COM O SERVIDOR, MAS, COM A DISCUSSÃO SOBRE NUMERO
* MAXIMO DE CLIENTES CONECTADOS, POLITENESS, BACKOFF E JITTER, ACABEI ADOTANDO O STOMP QUE MANTEM UMA CONEXÃO COM O
* SERVIDOR.
*
* ALGUMAS FUNÇÕES DO SERVER, COMO A DE BUSCAR POR GRUPOS E CRIAR UM GRUPO, MANTIVE SEM CONEXÃO NO SERVIDOR,
* APENAS A API REST, UMA VEZ QUE A PORÇÃO CHAVE DO PROJETO É A COMUNICAÇÃO POR MENSAGENS, MANTIVE ESSA PARTE COM
* CONEXÃO CONTINUA.
*
*
* */

public class ChatClientApplication {
    public static void main(String[] args) throws Exception {
        //TEMA P/ INTERFACE GRAFICA
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarculaLaf());
        } catch (UnsupportedLookAndFeelException ex) {
            System.err.println("Failed to initialize LaF");
        }
        StompSessionManager stompManager = new StompSessionManager();
        MessageHandler messageHandler = new MessageHandler();
        ChatApiService chatApiService = new ChatApiService(stompManager, messageHandler);

        ChatWindowController chatController = new ChatWindowController(stompManager, chatApiService, messageHandler);

        messageHandler.setListener(chatController);
        chatApiService.setListener(chatController);
        stompManager.setListener(chatController);

        stompManager.setApiService(chatApiService);
    }
}
