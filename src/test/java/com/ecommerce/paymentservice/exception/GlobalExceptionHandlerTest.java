package com.ecommerce.paymentservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleRuntimeException_returns404WithMessage() {
        ResponseEntity<Map<String, String>> response =
                handler.handleRuntimeException(new RuntimeException("Payment not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Payment not found");
    }

    @Test
    void handlePaymentAlreadyProcessedException_returns404() {
        PaymentAlreadyProcessedException ex = new PaymentAlreadyProcessedException("Order already paid");

        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("error")).isEqualTo("Order already paid");
    }

    @Test
    void handlePaymentAlreadyProcessedException_withCause() {
        PaymentAlreadyProcessedException ex =
                new PaymentAlreadyProcessedException("duplicate", new RuntimeException("cause"));

        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("error")).isEqualTo("duplicate");
    }

    @Test
    void handlePaymentNotFoundException_returns404() {
        PaymentNotFoundException ex = new PaymentNotFoundException("Payment 123 not found");

        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("error")).isEqualTo("Payment 123 not found");
    }
}
