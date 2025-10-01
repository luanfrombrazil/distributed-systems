package com.luanlana.chat_server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateGroupRequest {
    String roomName;
}
