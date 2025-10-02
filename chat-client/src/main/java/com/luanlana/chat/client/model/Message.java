package com.luanlana.chat.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data // Gera getters, setters, toString(), etc.
@NoArgsConstructor // Gera um construtor sem argumentos, necessário para o Jackson (conversor JSON)
@AllArgsConstructor // Gera um construtor com todos os argumentos
@JsonIgnoreProperties(ignoreUnknown = true) // Importante: ignora campos extras que o servidor possa enviar no futuro
public class Message {
    private Long id;
    private Instant timestamp;
    private String message;
    private String idemKey;
    private String nickname;
    private Group group;
}