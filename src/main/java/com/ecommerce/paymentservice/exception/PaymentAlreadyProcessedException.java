package com.ecommerce.paymentservice.exception;

public class PaymentAlreadyProcessedException extends RuntimeException {
    
    public PaymentAlreadyProcessedException(String message) {
        super(message);
    }
    
    public PaymentAlreadyProcessedException(String message, Throwable cause) {
        super(message, cause);
    }
}