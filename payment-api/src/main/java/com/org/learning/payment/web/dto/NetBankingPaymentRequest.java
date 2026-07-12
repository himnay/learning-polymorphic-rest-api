package com.org.learning.payment.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Net-banking payment — discriminator value {@code NET_BANKING}. */
public record NetBankingPaymentRequest(
        String type,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String bankCode)
        implements PaymentRequest {
}
