package com.luanlana.chat_server.service;

import com.luanlana.chat_server.dto.MessageRequest;
import com.luanlana.chat_server.log.LogWriter;
import com.luanlana.chat_server.model.Group;
import com.luanlana.chat_server.model.Message;
import com.luanlana.chat_server.repository.GroupRepository;
import com.luanlana.chat_server.repository.MessageRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MessageService {

    private LogWriter logger;

    {
        try {
            logger = new LogWriter("logs/serverlatency_log.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;
    private final GroupRepository groupRepository;

    public List<Message> findSince(Long groupId, Instant timestamp, Integer limit) {
        Pageable limitpage = PageRequest.of(0, limit);
        return messageRepository.findAllByGroupIdAndTimestampAfterOrderByTimestampAsc(groupId, timestamp, limitpage);
    }

    @Transactional
    public Message save(Long groupId, MessageRequest dto) {

        Optional<Message> existingMessage = messageRepository.findByIdemKey(dto.getIdemKey());
        if (existingMessage.isPresent()) {
            return existingMessage.get();
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("GRUPO COM ID NAO ENCONTRADO: " + groupId));

        Message newMessage = new Message();
        newMessage.setIdemKey(dto.getIdemKey());
        newMessage.setMessage(dto.getText());
        newMessage.setNickname(dto.getNickname());
        newMessage.setGroup(group);
        newMessage.setTimestamp(dto.getTimestampClient());
//        newMessage.setTimestamp(Instant.now());
        logger.print("MENSAGEM RECEBIDA, LATENCIA:" + Duration.between(dto.getTimestampClient(), Instant.now()).toMillis());

        Message savedMessage = messageRepository.save(newMessage);

        String destination = "/groups/" + groupId;
        messagingTemplate.convertAndSend(destination, savedMessage);

        return messageRepository.save(newMessage);
    }
}

