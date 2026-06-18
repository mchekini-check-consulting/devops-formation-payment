package com.ecommerce.paymentservice.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {
    // private UUID id;
    private UUID orderId;
    private String userId;
    private Double amount;
    private String status;
    // private LocalDateTime createdAt;
}