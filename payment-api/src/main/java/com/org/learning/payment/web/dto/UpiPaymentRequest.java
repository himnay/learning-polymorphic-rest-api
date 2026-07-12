package com.org.learning.payment.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** UPI payment — discriminator value {@code UPI}. */
public record UpiPaymentRequest(
        String type,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Pattern(regexp = ".+@.+", message = "must be a valid VPA like user@bank") String vpa)
        implements PaymentRequest {
}
