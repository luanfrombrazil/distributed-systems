package com.luanlana.chat_server.controller;

import com.luanlana.chat_server.dto.MessageRequest;
import com.luanlana.chat_server.model.Message;
import com.luanlana.chat_server.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/groups/{group_id}/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @GetMapping
    ResponseEntity<List<Message>> findMessagesSince(
            @PathVariable Long group_id,
            @RequestParam(name = "since") Instant cursor,
            @RequestParam(defaultValue = "10") Integer limit) {

        return new ResponseEntity<List<Message>>(messageService.findSince(group_id, cursor, limit), HttpStatus.OK);
    }

    @PostMapping
    ResponseEntity<Message> saveMessage(
            @PathVariable Long group_id,
            @RequestBody MessageRequest messageRequest
    ) {
        return new ResponseEntity<>(messageService.save(group_id, messageRequest), HttpStatus.CREATED); // CREATED (201) é mais semântico para criação
    }

}
