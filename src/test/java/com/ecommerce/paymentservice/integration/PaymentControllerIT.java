package com.ecommerce.paymentservice.integration;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.model.Payment;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PaymentControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-User-ID", "user-integration");
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private PaymentRequest newRequest() {
        PaymentRequest req = new PaymentRequest();
        req.setOrderId(UUID.randomUUID());
        req.setAmount(150.0);
        return req;
    }

    @Test
    void createPayment_returns201AndPersistsToDatabase() {
        PaymentRequest request = newRequest();

        ResponseEntity<PaymentResponse> response = restTemplate.exchange(
                "/api/payments", HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                PaymentResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getOrderId()).isEqualTo(request.getOrderId());
        assertThat(response.getBody().getUserId()).isEqualTo("user-integration");
        assertThat(response.getBody().getStatus()).isIn("SUCCESS", "FAILED");
        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    void createPayment_returns404_whenDuplicateOrder() {
        PaymentRequest request = newRequest();
        HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers());

        restTemplate.exchange("/api/payments", HttpMethod.POST, entity, PaymentResponse.class);
        ResponseEntity<Object> duplicate = restTemplate.exchange(
                "/api/payments", HttpMethod.POST, entity, Object.class);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createPayment_returns400_whenAmountNegative() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(UUID.randomUUID());
        request.setAmount(-50.0);

        ResponseEntity<Object> response = restTemplate.exchange(
                "/api/payments", HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createPayment_returns400_whenOrderIdNull() {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(100.0);

        ResponseEntity<Object> response = restTemplate.exchange(
                "/api/payments", HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getAllPayments_returnsAllPersistedPayments() {
        restTemplate.exchange("/api/payments", HttpMethod.POST, new HttpEntity<>(newRequest(), headers()), PaymentResponse.class);
        restTemplate.exchange("/api/payments", HttpMethod.POST, new HttpEntity<>(newRequest(), headers()), PaymentResponse.class);

        ResponseEntity<List<PaymentResponse>> response = restTemplate.exchange(
                "/api/payments", HttpMethod.GET,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getAllPayments_returnsEmptyListWhenNoneExist() {
        ResponseEntity<List<PaymentResponse>> response = restTemplate.exchange(
                "/api/payments", HttpMethod.GET,
                new HttpEntity<>(headers()),
                new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getPaymentById_returns200() {
        PaymentRequest request = newRequest();
        restTemplate.exchange("/api/payments", HttpMethod.POST, new HttpEntity<>(request, headers()), PaymentResponse.class);

        Payment saved = paymentRepository.findAll().get(0);

        ResponseEntity<PaymentResponse> response = restTemplate.exchange(
                "/api/payments/{id}", HttpMethod.GET,
                new HttpEntity<>(headers()),
                PaymentResponse.class,
                saved.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOrderId()).isEqualTo(request.getOrderId());
    }

    @Test
    void getPaymentById_returns404_whenNotFound() {
        ResponseEntity<Object> response = restTemplate.exchange(
                "/api/payments/{id}", HttpMethod.GET,
                new HttpEntity<>(headers()),
                Object.class,
                UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
