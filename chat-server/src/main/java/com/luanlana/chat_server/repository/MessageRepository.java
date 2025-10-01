package com.luanlana.chat_server.repository;

import com.luanlana.chat_server.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findAllByGroupIdAndTimestampAfterOrderByTimestampAsc(Long groupId, Instant since, Pageable pageable);

    Optional<Message> findByIdemKey(String idemKey);
}
