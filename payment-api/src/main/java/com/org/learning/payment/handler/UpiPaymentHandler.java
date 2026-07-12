package com.org.learning.payment.handler;

import com.org.learning.payment.web.dto.PaymentRequest;
import com.org.learning.payment.web.dto.PaymentResponse;
import com.org.learning.payment.web.dto.UpiPaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class UpiPaymentHandler implements PaymentHandler<UpiPaymentRequest> {

    @Override
    public boolean supports(PaymentRequest request) {
        return request instanceof UpiPaymentRequest;
    }

    @Override
    public PaymentResponse handle(UpiPaymentRequest request) {
        log.info("Sending UPI collect request of {} to {}", request.amount(), request.vpa());
        return new PaymentResponse(UUID.randomUUID(), request.type(), request.amount(), "COLLECT_REQUESTED");
    }
}
