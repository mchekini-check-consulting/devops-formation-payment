package com.ecommerce.paymentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.util.UUID;

@Data
public class PaymentRequest {
    @NotNull(message = "orderId is required")
    private UUID orderId;
    
    @NotNull(message = "userId is required")
    private String userId;
    
    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private Double amount;
}