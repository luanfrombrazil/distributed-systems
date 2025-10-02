package com.luanlana.chat_server.repository;

import com.luanlana.chat_server.model.Group;
import com.luanlana.chat_server.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

}