package com.luanlana.chat_server.service;

import com.luanlana.chat_server.dto.CreateGroupRequest;
import com.luanlana.chat_server.model.Group;
import com.luanlana.chat_server.repository.GroupRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepository;

    public List<Group> findAll() {
        return groupRepository.findAll();
    }

    public Group findById(Long id) {
        return groupRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Grupo não encontrado com o id: " + id));
    }

    public Group save(CreateGroupRequest groupRequest) {
        Group group = new Group();
        group.setRoomName(groupRequest.getRoomName());
        return groupRepository.save(group);
    }
}
