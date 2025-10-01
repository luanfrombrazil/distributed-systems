package com.luanlana.chat_server.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Data
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "timestamp")
    Instant timestamp;

    @Column(name = "message")
    String message;

    @Column(name = "idemKey")
    String idemKey;

    @Column(name = "nickname")
    String nickname;

    @ManyToOne
    @JoinColumn(name = "group_id", referencedColumnName = "id")
    Group group;
}
