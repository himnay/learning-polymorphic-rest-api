package com.org.learning.payment.handler;

import com.org.learning.payment.web.dto.NetBankingPaymentRequest;
import com.org.learning.payment.web.dto.PaymentRequest;
import com.org.learning.payment.web.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class NetBankingPaymentHandler implements PaymentHandler<NetBankingPaymentRequest> {

    @Override
    public boolean supports(PaymentRequest request) {
        return request instanceof NetBankingPaymentRequest;
    }

    @Override
    public PaymentResponse handle(NetBankingPaymentRequest request) {
        log.info("Initiating net-banking redirect of {} via bank {}", request.amount(), request.bankCode());
        return new PaymentResponse(UUID.randomUUID(), request.type(), request.amount(), "REDIRECT_INITIATED");
    }
}
