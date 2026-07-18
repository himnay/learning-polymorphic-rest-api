package com.org.learning.payment.web.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;

/**
 * Polymorphic payment request — one endpoint, many concrete shapes.
 *
 * <p>{@code EXISTING_PROPERTY} + {@code visible = true} keeps {@code type} a real, validatable
 * field on each record instead of Jackson-internal metadata. Discriminator values are logical
 * names mapped through an explicit {@link JsonSubTypes} allow-list — never class names
 * ({@code Id.CLASS} is a gadget-chain RCE vector on internet-facing DTOs).
 *
 * <p>The sealed interface makes the compiler prove that the {@code @JsonSubTypes} list and the
 * business {@code switch} in {@code PaymentService} cover the same set of subtypes.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CardPaymentRequest.class, name = "CARD"),
        @JsonSubTypes.Type(value = UpiPaymentRequest.class, name = "UPI"),
        @JsonSubTypes.Type(value = NetBankingPaymentRequest.class, name = "NET_BANKING")
})
public sealed interface PaymentRequest
        permits CardPaymentRequest, UpiPaymentRequest, NetBankingPaymentRequest {

    String type();

    BigDecimal amount();
}
