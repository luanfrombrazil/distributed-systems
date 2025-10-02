package com.luanlana.chat.client.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MessageRequest {
    private String idemKey;
    private String text;
    private String nickname;
    private Instant timestampClient;
}