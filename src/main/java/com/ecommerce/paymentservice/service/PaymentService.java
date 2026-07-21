package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.model.Payment;
import com.ecommerce.paymentservice.model.PaymentStatus;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.ecommerce.paymentservice.exception.PaymentAlreadyProcessedException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final Random random = new Random();
    private final Counter paymentsSuccessCounter;
    private final Counter paymentsFailedCounter;

    public PaymentService(PaymentRepository paymentRepository, MeterRegistry registry) {
        this.paymentRepository = paymentRepository;
        this.paymentsSuccessCounter = Counter.builder("payments_success_total")
                .description("Total number of successful payments")
                .register(registry);
        this.paymentsFailedCounter = Counter.builder("payments_failed_total")
                .description("Total number of failed payments")
                .register(registry);
    }
    
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for order: {}", request.getOrderId());
        


        // Vérifier si la commande a déjà été payée
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(request.getOrderId());
        if (existingPayment.isPresent()) {
            log.warn("Payment already processed for order: {}", request.getOrderId());
            throw new PaymentAlreadyProcessedException("Order " + request.getOrderId() + " has already been paid");
        }





        // Simulation du paiement (80% de succès)
        boolean isSuccess = random.nextDouble() < 0.8;
        PaymentStatus status = isSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;

        if (isSuccess) {
            paymentsSuccessCounter.increment();
        } else {
            paymentsFailedCounter.increment();
        }
        
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .status(status)
                .build();
        
        // Payment savedPayment = paymentRepository.save(payment);
        // log.info("Payment processed with status: {} for order: {}", status, request.getOrderId());
        
        // return mapToResponse(savedPayment);
    
    
    
        try {
            Payment savedPayment = paymentRepository.save(payment);
            log.info("Payment processed with status: {} for order: {}", status, request.getOrderId());
            return mapToResponse(savedPayment);
        } catch (DataIntegrityViolationException e) {
            log.error("Duplicate payment attempt for order: {}", request.getOrderId());
            throw new PaymentAlreadyProcessedException("Order " + request.getOrderId() + " has already been paid");
        }
    
    
    
    }
    
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public PaymentResponse getPaymentById(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
        return mapToResponse(payment);
    }
    



    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                // .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus().toString())
                // .createdAt(payment.getCreatedAt())
                .build();
    }
}