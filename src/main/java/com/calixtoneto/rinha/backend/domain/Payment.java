package com.calixtoneto.rinha.backend.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Document(collection = "payments")
public class Payment {
    @Id
    private UUID id;
    private UUID correlationId;
    private Double amount;
    private String processor;
    private LocalDateTime requestedAt;
}