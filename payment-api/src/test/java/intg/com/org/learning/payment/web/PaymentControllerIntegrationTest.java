package com.org.learning.payment.web;

import com.org.learning.payment.web.dto.CardPaymentRequest;
import com.org.learning.payment.web.dto.NetBankingPaymentRequest;
import com.org.learning.payment.web.dto.PaymentResponse;
import com.org.learning.payment.web.dto.UpiPaymentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void cardPayment_fullFlow_createThenFetch() throws Exception {
        var request = new CardPaymentRequest("CARD", new BigDecimal("499.00"),
                "4111111111111111", "123");

        var body = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CARD"))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andReturn().getResponse().getContentAsString();

        var created = objectMapper.readValue(body, PaymentResponse.class);
        assertThat(created.id()).isNotNull();
        assertThat(created.amount()).isEqualByComparingTo("499.00");

        mockMvc.perform(get("/api/v1/payments/{id}", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id().toString()))
                .andExpect(jsonPath("$.type").value("CARD"))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));
    }

    @Test
    void upiPayment_fullFlow_createThenFetch() throws Exception {
        var request = new UpiPaymentRequest("UPI", new BigDecimal("250.50"), "user@upi");

        var body = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("UPI"))
                .andExpect(jsonPath("$.status").value("COLLECT_REQUESTED"))
                .andReturn().getResponse().getContentAsString();

        var created = objectMapper.readValue(body, PaymentResponse.class);

        mockMvc.perform(get("/api/v1/payments/{id}", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("UPI"));
    }

    @Test
    void netBankingPayment_fullFlow_createThenFetch() throws Exception {
        var request = new NetBankingPaymentRequest("NET_BANKING", new BigDecimal("1200.00"), "HDFC");

        var body = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("NET_BANKING"))
                .andExpect(jsonPath("$.status").value("REDIRECT_INITIATED"))
                .andReturn().getResponse().getContentAsString();

        var created = objectMapper.readValue(body, PaymentResponse.class);

        mockMvc.perform(get("/api/v1/payments/{id}", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("NET_BANKING"));
    }

    @Test
    void unknownDiscriminator_returns400ListingAllowedTypes() throws Exception {
        var body = """
                { "type": "CRYPTO", "amount": 42.00 }
                """;

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("CRYPTO")))
                .andExpect(jsonPath("$.detail", containsString("CARD, UPI, NET_BANKING")));
    }

    @Test
    void wrongSubtypeFields_cardWithoutCvv_returns400() throws Exception {
        // UPI-shaped fields under a CARD discriminator — card constraints must reject it
        var body = """
                { "type": "CARD", "amount": 10.00, "vpa": "user@upi" }
                """;

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("cardNumber")))
                .andExpect(jsonPath("$.detail", containsString("cvv")));
    }

    @Test
    void getPayment_unknownId_returns404() throws Exception {
        var id = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/payments/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString(id.toString())));
    }
}
