package com.org.learning.payment.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Card payment — discriminator value {@code CARD}. Card-only constraints (cardNumber, cvv)
 * live here, not in a global if-soup: {@code @Valid} cascades after Jackson resolves the type.
 */
public record CardPaymentRequest(
        String type,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Pattern(regexp = "\\d{16}", message = "must be exactly 16 digits") String cardNumber,
        @NotBlank @Pattern(regexp = "\\d{3}", message = "must be exactly 3 digits") String cvv)
        implements PaymentRequest {
}
