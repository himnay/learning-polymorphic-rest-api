package com.org.learning.payment.web;

import com.org.learning.payment.service.PaymentService;
import com.org.learning.payment.web.dto.PaymentRequest;
import com.org.learning.payment.web.dto.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Single polymorphic endpoint — Jackson resolves the concrete {@link PaymentRequest} subtype
 * from the {@code type} discriminator, then {@code @Valid} cascades the subtype's constraints.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    /** Creates. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@RequestBody @Valid PaymentRequest request) {
        return paymentService.create(request);
    }

    /** Returns the get. */
    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        return paymentService.get(id);
    }
}
