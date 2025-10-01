package com.luanlana.chat_server.service;

import com.luanlana.chat_server.dto.MessageRequest;
import com.luanlana.chat_server.model.Group;
import com.luanlana.chat_server.model.Message;
import com.luanlana.chat_server.repository.GroupRepository;
import com.luanlana.chat_server.repository.MessageRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MessageService {
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
                .orElseThrow(() -> new EntityNotFoundException("Grupo não encontrado com o id: " + groupId));

        Message newMessage = new Message();
        newMessage.setIdemKey(dto.getIdemKey());
        newMessage.setMessage(dto.getText());
        newMessage.setNickname(dto.getNickname());
        newMessage.setGroup(group);
        newMessage.setTimestamp(Instant.now());
        return messageRepository.save(newMessage);
    }
}

