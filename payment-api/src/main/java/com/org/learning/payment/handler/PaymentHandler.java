package com.org.learning.payment.handler;

import com.org.learning.payment.web.dto.PaymentRequest;
import com.org.learning.payment.web.dto.PaymentResponse;

/**
 * Strategy for processing one concrete payment shape. New payment methods plug in as new
 * {@code @Component} beans (open/closed) — the sealed interface then forces the dispatch
 * switch in {@code PaymentService} to acknowledge them at compile time.
 */
public interface PaymentHandler<T extends PaymentRequest> {

    /** Whether this strategy processes the given request's concrete type. */
    boolean supports(PaymentRequest request);

    /** Process the payment and return the response with the discriminator echoed. */
    PaymentResponse handle(T request);
}
