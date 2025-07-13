package com.calixtoneto.rinha.backend.service;

import com.calixtoneto.rinha.backend.dto.PaymentRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;

@Service
public class PaymentProcessorClient {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${payment.processor.default.url}")
    private String defaultUrl;

    @Value("${payment.processor.fallback.url}")
    private String fallbackUrl;

    @Autowired
    @Qualifier("reactiveRedisTemplate")
    private ReactiveRedisTemplate<String, String> redisTemplate;

    private WebClient webClientDefault;
    private WebClient webClientFallback;

    @Autowired
    public void initWebClients() {
        this.webClientDefault = webClientBuilder.baseUrl(defaultUrl).build();
        this.webClientFallback = webClientBuilder.baseUrl(fallbackUrl).build();
    }

    @CircuitBreaker(name = "paymentProcessor")
    @Retry(name = "paymentProcessor")
    public Mono<Map<String, Object>> processPayment(PaymentRequest request, String processor) {
        Map<String, Object> body = new HashMap<>();
        body.put("correlationId", request.getCorrelationId());
        body.put("amount", request.getAmount());
        body.put("requestedAt", java.time.LocalDateTime.now().toString());
        WebClient client = "default".equals(processor) ? webClientDefault : webClientFallback;
        return client.post()
                .uri("/payments")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    public Mono<Map<String, Object>> checkHealth(String processor) {
        String redisKey = "health-check:" + processor;
        return redisTemplate.opsForValue().get(redisKey)
                .flatMap(value -> {
                    Map<String, Object> health = new HashMap<>();
                    health.put("failing", Boolean.valueOf(value.split(":")[0]));
                    health.put("minResponseTime", Integer.valueOf(value.split(":")[1]));
                    return Mono.just(health);
                })
                .switchIfEmpty(
                        ("default".equals(processor) ? webClientDefault : webClientFallback).get()
                                .uri("/payments/service-health")
                                .retrieve()
                                .onStatus(status -> status == HttpStatus.TOO_MANY_REQUESTS,
                                        response -> Mono.just(new RuntimeException("Too Many Requests")))
                                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                                .onErrorResume(RuntimeException.class, e -> {
                                    if (e.getMessage().equals("Too Many Requests")) {
                                        Map<String, Object> health = new HashMap<>();
                                        health.put("failing", true);
                                        health.put("minResponseTime", 1000);
                                        return Mono.just(health);
                                    }
                                    return Mono.error(e);
                                })
                                .flatMap(health -> redisTemplate.opsForValue()
                                        .set(redisKey, health.get("failing") + ":" + health.get("minResponseTime"), Duration.ofSeconds(5))
                                        .thenReturn(health))
                );
    }
}