package com.calixtoneto.rinha.backend.api;

import com.calixtoneto.rinha.backend.dto.PaymentRequest;
import com.calixtoneto.rinha.backend.dto.PaymentsSummaryResponse;
import com.calixtoneto.rinha.backend.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/payments-summary")
    public Mono<ResponseEntity<PaymentsSummaryResponse>> getPaymentsSummary(
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to) {
        logger.info("Manual controller: Received request to get payments summary: from={}, to={}", from, to);
        LocalDateTime fromDate = from != null ? LocalDateTime.parse(from) : null;
        LocalDateTime toDate = to != null ? LocalDateTime.parse(to) : null;
        return paymentService.getPaymentsSummary(fromDate, toDate)
                .map(ResponseEntity::ok)
                .onErrorMap(IllegalArgumentException.class, e -> {
                    logger.error("Bad request: {}", e.getMessage());
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
                })
                .onErrorMap(Exception.class, e -> {
                    logger.error("Internal error: {}", e.getMessage());
                    return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", e);
                });
    }

    @PostMapping("/payments")
    public Mono<Map<String, Object>> createPayment(@RequestBody PaymentRequest paymentRequest) {
        logger.info("Manual controller: Received request to create payment: {}", paymentRequest);
        return paymentService.createPayment(paymentRequest)
                .onErrorMap(IllegalArgumentException.class, e -> {
                    logger.error("Bad request: {}", e.getMessage());
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
                })
                .onErrorMap(IllegalStateException.class, e -> {
                    logger.error("Processor unavailable: {}", e.getMessage());
                    return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Both processors are unavailable", e);
                });
    }
}