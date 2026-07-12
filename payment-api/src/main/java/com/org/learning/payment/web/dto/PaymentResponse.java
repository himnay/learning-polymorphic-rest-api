package com.org.learning.payment.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payment response with the discriminator echoed back — clients dispatch on {@code type}
 * without sniffing fields.
 */
public record PaymentResponse(
        UUID id,
        String type,
        BigDecimal amount,
        String status) {
}
