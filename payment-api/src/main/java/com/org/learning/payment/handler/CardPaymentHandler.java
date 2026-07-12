package com.org.learning.payment.handler;

import com.org.learning.payment.web.dto.CardPaymentRequest;
import com.org.learning.payment.web.dto.PaymentRequest;
import com.org.learning.payment.web.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class CardPaymentHandler implements PaymentHandler<CardPaymentRequest> {

    @Override
    public boolean supports(PaymentRequest request) {
        return request instanceof CardPaymentRequest;
    }

    @Override
    public PaymentResponse handle(CardPaymentRequest request) {
        log.info("Authorizing card payment of {} on card ending {}",
                request.amount(), request.cardNumber().substring(12));
        return new PaymentResponse(UUID.randomUUID(), request.type(), request.amount(), "AUTHORIZED");
    }
}
