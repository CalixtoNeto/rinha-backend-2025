package com.calixtoneto.rinha.backend.repository;

import com.calixtoneto.rinha.backend.domain.Payment;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PaymentRepository extends ReactiveMongoRepository<Payment, UUID> {

    Flux<Payment> findByRequestedAtBetween(LocalDateTime from, LocalDateTime to);

    Mono<Payment> findByCorrelationId(UUID correlationId);
}