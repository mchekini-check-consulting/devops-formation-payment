package com.ecommerce.paymentservice.repository;

import com.ecommerce.paymentservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.Optional; 


@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // Spring Data JPA va implémenter automatiquement cette méthode
    Optional<Payment> findByOrderId(UUID orderId);
    
    // Autres méthodes utiles
    boolean existsByOrderId(UUID orderId);

}