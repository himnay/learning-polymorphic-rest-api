package com.org.learning.payment.web;

import com.org.learning.payment.exception.PaymentNotFoundException;
import com.org.learning.payment.service.PaymentService;
import com.org.learning.payment.web.dto.CardPaymentRequest;
import com.org.learning.payment.web.dto.NetBankingPaymentRequest;
import com.org.learning.payment.web.dto.PaymentRequest;
import com.org.learning.payment.web.dto.PaymentResponse;
import com.org.learning.payment.web.dto.UpiPaymentRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    // --- discriminator round-trips: record -> JSON -> concrete subtype ---

    @Test
    void createPayment_cardType_roundTripsToCardSubtype() throws Exception {
        var request = new CardPaymentRequest("CARD", new BigDecimal("499.00"),
                "4111111111111111", "123");
        var response = new PaymentResponse(UUID.randomUUID(), "CARD",
                new BigDecimal("499.00"), "AUTHORIZED");
        when(paymentService.create(any(PaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.type").value("CARD"))
                .andExpect(jsonPath("$.amount").value(499.00))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));

        var captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentService).create(captor.capture());
        assertThat(captor.getValue())
                .isInstanceOf(CardPaymentRequest.class)
                .isEqualTo(request);
    }

    @Test
    void createPayment_upiType_roundTripsToUpiSubtype() throws Exception {
        var request = new UpiPaymentRequest("UPI", new BigDecimal("250.50"), "user@upi");
        var response = new PaymentResponse(UUID.randomUUID(), "UPI",
                new BigDecimal("250.50"), "COLLECT_REQUESTED");
        when(paymentService.create(any(PaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("UPI"))
                .andExpect(jsonPath("$.status").value("COLLECT_REQUESTED"));

        var captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentService).create(captor.capture());
        assertThat(captor.getValue())
                .isInstanceOf(UpiPaymentRequest.class)
                .isEqualTo(request);
    }

    @Test
    void createPayment_netBankingType_roundTripsToNetBankingSubtype() throws Exception {
        var request = new NetBankingPaymentRequest("NET_BANKING", new BigDecimal("1200.00"), "HDFC");
        var response = new PaymentResponse(UUID.randomUUID(), "NET_BANKING",
                new BigDecimal("1200.00"), "REDIRECT_INITIATED");
        when(paymentService.create(any(PaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("NET_BANKING"))
                .andExpect(jsonPath("$.status").value("REDIRECT_INITIATED"));

        var captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentService).create(captor.capture());
        assertThat(captor.getValue())
                .isInstanceOf(NetBankingPaymentRequest.class)
                .isEqualTo(request);
    }

    // --- error shapes ---

    @Test
    void createPayment_unknownType_returns400ListingAllowedTypes() throws Exception {
        var body = """
                { "type": "WALLET", "amount": 10.00 }
                """;

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("WALLET")))
                .andExpect(jsonPath("$.detail", containsString("CARD")))
                .andExpect(jsonPath("$.detail", containsString("UPI")))
                .andExpect(jsonPath("$.detail", containsString("NET_BANKING")));
    }

    @Test
    void createPayment_cardWithInvalidFields_returns400WithFieldErrors() throws Exception {
        var invalid = new CardPaymentRequest("CARD", new BigDecimal("-5"), "1234", "12345");

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("amount")))
                .andExpect(jsonPath("$.detail", containsString("cardNumber")))
                .andExpect(jsonPath("$.detail", containsString("cvv")));
    }

    @Test
    void createPayment_upiWithInvalidVpa_returns400() throws Exception {
        var invalid = new UpiPaymentRequest("UPI", new BigDecimal("10.00"), "not-a-vpa");

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("vpa")));
    }

    @Test
    void createPayment_netBankingWithBlankBankCode_returns400() throws Exception {
        var invalid = new NetBankingPaymentRequest("NET_BANKING", new BigDecimal("10.00"), "");

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("bankCode")));
    }

    @Test
    void getPayment_unknownId_returns404() throws Exception {
        var id = UUID.randomUUID();
        when(paymentService.get(id)).thenThrow(new PaymentNotFoundException(id));

        mockMvc.perform(get("/api/v1/payments/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString(id.toString())));
    }
}
