package com.luanlana.chat.client.api;

import com.luanlana.chat.client.model.Message;

import java.util.ArrayList;
import java.util.List;

public interface MessageListener {
    void onMessageReceived(Message message);
    void onHistoryReceived(List<Message> history);
}
