package com.org.learning.payment.service;

import com.org.learning.payment.exception.PaymentNotFoundException;
import com.org.learning.payment.handler.PaymentHandler;
import com.org.learning.payment.web.dto.CardPaymentRequest;
import com.org.learning.payment.web.dto.NetBankingPaymentRequest;
import com.org.learning.payment.web.dto.PaymentRequest;
import com.org.learning.payment.web.dto.PaymentResponse;
import com.org.learning.payment.web.dto.UpiPaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispatches each concrete payment shape to its {@link PaymentHandler} strategy.
 *
 * <p>The {@code switch} has no {@code default} arm on purpose: {@link PaymentRequest} is
 * sealed, so adding a fourth subtype breaks compilation here until it is handled — the
 * runtime {@code @JsonSubTypes} allow-list and the compile-time dispatch can never drift.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final List<PaymentHandler<? extends PaymentRequest>> handlers;
    private final Map<UUID, PaymentResponse> store = new ConcurrentHashMap<>();

    public PaymentResponse create(PaymentRequest request) {
        var response = switch (request) {
            case CardPaymentRequest card -> handlerFor(card).handle(card);
            case UpiPaymentRequest upi -> handlerFor(upi).handle(upi);
            case NetBankingPaymentRequest netBanking -> handlerFor(netBanking).handle(netBanking);
        };
        store.put(response.id(), response);
        return response;
    }

    public PaymentResponse get(UUID id) {
        var response = store.get(id);
        if (response == null) {
            throw new PaymentNotFoundException(id);
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    private <T extends PaymentRequest> PaymentHandler<T> handlerFor(T request) {
        return (PaymentHandler<T>) handlers.stream()
                .filter(handler -> handler.supports(request))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No handler registered for payment type: " + request.type()));
    }
}
