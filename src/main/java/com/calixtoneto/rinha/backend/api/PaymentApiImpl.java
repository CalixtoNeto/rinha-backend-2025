package com.calixtoneto.rinha.backend.api;

import com.calixtoneto.rinha.backend.dto.PaymentRequest;
import com.calixtoneto.rinha.backend.dto.PaymentsSummaryResponse;
import com.calixtoneto.rinha.backend.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;

@RestController
public class PaymentApiImpl implements PaymentsApi {

    private static final Logger logger = LoggerFactory.getLogger(PaymentApiImpl.class);

    @Autowired
    private PaymentService paymentService;


    @Override
    public Mono<ResponseEntity<Map<String, Object>>> createPayment(
            @Valid Mono<PaymentRequest> paymentRequest,
            ServerWebExchange exchange
    ) {
        return paymentRequest
                .flatMap(req -> {
                    logger.info("Received request to create payment: {}", req);
                    return paymentService.createPayment(req)
                            .map(resultMap -> ResponseEntity.ok(resultMap))
                            .onErrorMap(IllegalArgumentException.class,
                                    e -> new ResponseStatusException(
                                            HttpStatus.BAD_REQUEST, e.getMessage()))
                            .onErrorMap(IllegalStateException.class,
                                    e -> new ResponseStatusException(
                                            HttpStatus.INTERNAL_SERVER_ERROR,
                                            "Both processors are unavailable", e));
                });
    }

    @Override
    public Mono<ResponseEntity<PaymentsSummaryResponse>> getPaymentsSummary(
            OffsetDateTime from,
            OffsetDateTime to,
            ServerWebExchange exchange
    ) {
        logger.info("Received request to get payments summary: from={}, to={}", from, to);
        LocalDateTime fromDate = (from != null ? from.toLocalDateTime() : null);
        LocalDateTime toDate   = (to   != null ? to.toLocalDateTime()   : null);
        return paymentService.getPaymentsSummary(fromDate, toDate)
                .map(summary -> ResponseEntity.ok(summary))
                .onErrorMap(IllegalArgumentException.class,
                        e -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, e.getMessage()))
                .onErrorMap(IllegalStateException.class,
                        e -> new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Internal error", e));
    }
}