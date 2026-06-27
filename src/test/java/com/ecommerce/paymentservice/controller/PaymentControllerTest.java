package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.exception.PaymentAlreadyProcessedException;
import com.ecommerce.paymentservice.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PaymentController.class)
@TestPropertySource(properties = {
        "cors.allowed-origins=*",
        "cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS,PATCH",
        "cors.allowed-headers=*",
        "cors.allow-credentials=false",
        "cors.mapping-path=/api/**"
})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    private PaymentRequest validRequest() {
        PaymentRequest req = new PaymentRequest();
        req.setOrderId(UUID.randomUUID());
        req.setAmount(100.0);
        return req;
    }

    @Test
    void createPayment_returns201() throws Exception {
        PaymentRequest request = validRequest();
        PaymentResponse response = PaymentResponse.builder()
                .orderId(request.getOrderId()).userId("user-1").amount(100.0).status("SUCCESS").build();
        when(paymentService.processPayment(any())).thenReturn(response);

        mockMvc.perform(post("/api/payments")
                        .header("X-User-ID", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(100.0));
    }

    @Test
    void createPayment_returns400_whenBodyInvalid() throws Exception {
        PaymentRequest request = new PaymentRequest(); // orderId and amount missing

        mockMvc.perform(post("/api/payments")
                        .header("X-User-ID", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_returns400_whenAmountNegative() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(UUID.randomUUID());
        request.setAmount(-10.0);

        mockMvc.perform(post("/api/payments")
                        .header("X-User-ID", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_returns400_whenHeaderMissing() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_returns404_whenOrderAlreadyProcessed() throws Exception {
        when(paymentService.processPayment(any()))
                .thenThrow(new PaymentAlreadyProcessedException("already paid"));

        mockMvc.perform(post("/api/payments")
                        .header("X-User-ID", "user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("already paid"));
    }

    @Test
    void getAllPayments_returns200() throws Exception {
        when(paymentService.getAllPayments()).thenReturn(List.of(
                PaymentResponse.builder().orderId(UUID.randomUUID()).userId("u1").amount(50.0).status("SUCCESS").build()
        ));

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[0].amount").value(50.0));
    }

    @Test
    void getAllPayments_returnsEmptyList() throws Exception {
        when(paymentService.getAllPayments()).thenReturn(List.of());

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getPaymentById_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        PaymentResponse response = PaymentResponse.builder()
                .orderId(UUID.randomUUID()).userId("u1").amount(75.0).status("FAILED").build();
        when(paymentService.getPaymentById(id)).thenReturn(response);

        mockMvc.perform(get("/api/payments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void getPaymentById_returns404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(paymentService.getPaymentById(id)).thenThrow(new RuntimeException("Payment not found with id: " + id));

        mockMvc.perform(get("/api/payments/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
