package com.luanlana.chat.client;

import com.luanlana.chat.client.api.ChatApiService;
import com.luanlana.chat.client.api.MessageHandler;
import com.luanlana.chat.client.api.StompSessionManager;
import com.luanlana.chat.client.ui.ChatWindowController;

import javax.swing.*;

public class ChatClientApplication {
    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarculaLaf());
        } catch (UnsupportedLookAndFeelException e) {
            System.err.println("Failed to initialize LaF");
        }
        StompSessionManager stompManager = new StompSessionManager();
        MessageHandler messageHandler = new MessageHandler();
        ChatApiService chatApiService = new ChatApiService(stompManager, messageHandler);
        ChatWindowController chatController = new ChatWindowController(stompManager, chatApiService, messageHandler);
        messageHandler.setMessageListener(chatController);
    }
}
