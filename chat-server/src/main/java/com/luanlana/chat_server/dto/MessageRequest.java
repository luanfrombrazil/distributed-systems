package com.luanlana.chat_server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class MessageRequest {
    String idemKey;
    String text;
    String nickname;
    Instant timestampClient;
}
