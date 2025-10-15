package com.luanlana.chat.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/*
BASICAMENTE UMA REPLICA DO MODEL MESSAGE DO SERVER
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    private Long id;
    private Instant timestamp;
    private String message;
    private String idemKey;
    private String nickname;
    private Group group;
}