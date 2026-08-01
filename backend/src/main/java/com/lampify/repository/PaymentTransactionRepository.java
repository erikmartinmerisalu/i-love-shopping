package com.lampify.repository;

import com.lampify.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByProviderPaymentId(String providerPaymentId);

    List<PaymentTransaction> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
