package com.luanlana.chat_server.controller;

import com.luanlana.chat_server.dto.CreateGroupRequest;
import com.luanlana.chat_server.model.Group;
import com.luanlana.chat_server.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;

    @GetMapping
    public ResponseEntity<List<Group>> findAll() {
        return new ResponseEntity<List<Group>>(groupService.findAll(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    ResponseEntity<Group> findById(@PathVariable Long id) {
        return new ResponseEntity<Group>(groupService.findById(id), HttpStatus.OK);
    }

    @PostMapping
    ResponseEntity<Group> insertGroup(@RequestBody CreateGroupRequest group) {
        return new ResponseEntity<Group>(groupService.save(group), HttpStatus.OK);
    }
}
