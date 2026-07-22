package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.exception.PaymentAlreadyProcessedException;
import com.ecommerce.paymentservice.model.Payment;
import com.ecommerce.paymentservice.model.PaymentStatus;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentService paymentService;

    private UUID orderId;
    private PaymentRequest request;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, new SimpleMeterRegistry());
        orderId = UUID.randomUUID();
        request = new PaymentRequest();
        request.setOrderId(orderId);
        request.setUserId("user-123");
        request.setAmount(99.99);
    }

    @Test
    void processPayment_returnsSuccessResponse() {
        Random mockRandom = mock(Random.class);
        when(mockRandom.nextDouble()).thenReturn(0.5); // 0.5 < 0.8 → SUCCESS
        ReflectionTestUtils.setField(paymentService, "random", mockRandom);

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        Payment saved = Payment.builder()
                .id(UUID.randomUUID()).orderId(orderId)
                .userId("user-123").amount(99.99).status(PaymentStatus.SUCCESS).build();
        when(paymentRepository.save(any())).thenReturn(saved);

        PaymentResponse response = paymentService.processPayment(request);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getUserId()).isEqualTo("user-123");
        assertThat(response.getAmount()).isEqualTo(99.99);
    }

    @Test
    void processPayment_returnsFailedResponseWhenRandomAboveThreshold() {
        Random mockRandom = mock(Random.class);
        when(mockRandom.nextDouble()).thenReturn(0.9); // 0.9 >= 0.8 → FAILED
        ReflectionTestUtils.setField(paymentService, "random", mockRandom);

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        Payment saved = Payment.builder()
                .id(UUID.randomUUID()).orderId(orderId)
                .userId("user-123").amount(99.99).status(PaymentStatus.FAILED).build();
        when(paymentRepository.save(any())).thenReturn(saved);

        PaymentResponse response = paymentService.processPayment(request);

        assertThat(response.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void processPayment_throwsWhenOrderAlreadyExists() {
        Payment existing = Payment.builder().orderId(orderId).status(PaymentStatus.SUCCESS).build();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> paymentService.processPayment(request))
                .isInstanceOf(PaymentAlreadyProcessedException.class)
                .hasMessageContaining(orderId.toString());
    }

    @Test
    void processPayment_throwsOnDataIntegrityViolation() {
        Random mockRandom = mock(Random.class);
        when(mockRandom.nextDouble()).thenReturn(0.5);
        ReflectionTestUtils.setField(paymentService, "random", mockRandom);

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> paymentService.processPayment(request))
                .isInstanceOf(PaymentAlreadyProcessedException.class)
                .hasMessageContaining(orderId.toString());
    }

    @Test
    void getAllPayments_returnsMappedList() {
        List<Payment> payments = List.of(
                Payment.builder().orderId(UUID.randomUUID()).userId("u1").amount(10.0).status(PaymentStatus.SUCCESS).build(),
                Payment.builder().orderId(UUID.randomUUID()).userId("u2").amount(20.0).status(PaymentStatus.FAILED).build()
        );
        when(paymentRepository.findAll()).thenReturn(payments);

        List<PaymentResponse> result = paymentService.getAllPayments();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo("SUCCESS");
        assertThat(result.get(1).getStatus()).isEqualTo("FAILED");
    }

    @Test
    void getAllPayments_returnsEmptyList() {
        when(paymentRepository.findAll()).thenReturn(List.of());

        assertThat(paymentService.getAllPayments()).isEmpty();
    }

    @Test
    void getPaymentById_returnsPayment() {
        UUID id = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(id).orderId(orderId).userId("user-123").amount(50.0).status(PaymentStatus.SUCCESS).build();
        when(paymentRepository.findById(id)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentById(id);

        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void getPaymentById_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(paymentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(id.toString());
    }
}
