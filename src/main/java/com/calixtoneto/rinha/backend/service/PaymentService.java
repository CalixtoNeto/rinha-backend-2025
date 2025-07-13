package com.calixtoneto.rinha.backend.service;

import com.calixtoneto.rinha.backend.domain.Payment;
import com.calixtoneto.rinha.backend.dto.DefaultSummary;
import com.calixtoneto.rinha.backend.dto.FallbackSummary;
import com.calixtoneto.rinha.backend.dto.PaymentRequest;
import com.calixtoneto.rinha.backend.dto.PaymentsSummaryResponse;
import com.calixtoneto.rinha.backend.repository.PaymentRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentProcessorClient paymentProcessorClient;

    @Autowired
    private ModelMapper modelMapper;

    public Mono<Map<String, Object>> createPayment(PaymentRequest request) {
        UUID correlationId = UUID.fromString(request.getCorrelationId().toString());
        return paymentRepository.findByCorrelationId(correlationId)
                .flatMap(existing -> Mono.<Map<String, Object>>error(new IllegalArgumentException("Duplicate correlationId")))
                .switchIfEmpty(
                        Mono.zip(
                                paymentProcessorClient.checkHealth("default"),
                                paymentProcessorClient.checkHealth("fallback")
                        ).flatMap(tuple -> {
                            Map<String, Object> defaultHealth = tuple.getT1();
                            Map<String, Object> fallbackHealth = tuple.getT2();
                            String processor = selectProcessor(defaultHealth, fallbackHealth);
                            Payment payment = new Payment();
                            payment.setId(UUID.randomUUID());
                            payment.setCorrelationId(correlationId);
                            payment.setAmount(request.getAmount());
                            payment.setProcessor(processor);
                            payment.setRequestedAt(LocalDateTime.now());
                            return paymentRepository.save(payment)
                                    .then(paymentProcessorClient.processPayment(request, processor))
                                    .thenReturn(Map.of("message", "payment processed"));
                        })
                ).onErrorMap(e -> new IllegalArgumentException("Payment processing failed", e));
    }

    public Mono<PaymentsSummaryResponse> getPaymentsSummary(LocalDateTime from, LocalDateTime to) {
        return paymentRepository.findByRequestedAtBetween(
                from != null ? from : LocalDateTime.now().minusDays(30),
                to != null ? to : LocalDateTime.now()
        ).collect(() -> new PaymentsSummaryResponse(),
                (response, payment) -> {
                    var defaultSummary = response.getDefault() != null ? response.getDefault() : new DefaultSummary();
                    var fallbackSummary = response.getFallback() != null ? response.getFallback() : new FallbackSummary();
                    if ("default".equals(payment.getProcessor())) {
                        defaultSummary.setTotalRequests(defaultSummary.getTotalRequests() + 1);
                        defaultSummary.setTotalAmount(defaultSummary.getTotalAmount() + payment.getAmount());
                    } else {
                        fallbackSummary.setTotalRequests(fallbackSummary.getTotalRequests() + 1);
                        fallbackSummary.setTotalAmount(fallbackSummary.getTotalAmount() + payment.getAmount());
                    }
                    response.setDefault(defaultSummary);
                    response.setFallback(fallbackSummary);
                });
    }

    private String selectProcessor(Map<String, Object> defaultHealth, Map<String, Object> fallbackHealth) {
        boolean defaultFailing = (boolean) defaultHealth.get("failing");
        boolean fallbackFailing = (boolean) fallbackHealth.get("failing");
        int defaultResponseTime = (int) defaultHealth.get("minResponseTime");
        int fallbackResponseTime = (int) fallbackHealth.get("minResponseTime");

        if (!defaultFailing && defaultResponseTime <= 100) {
            return "default";
        } else if (!fallbackFailing) {
            return "fallback";
        }
        throw new IllegalStateException("Both processors are failing");
    }
}