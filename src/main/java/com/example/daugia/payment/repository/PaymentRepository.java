package com.example.daugia.payment.repository;

import com.example.daugia.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByVnpayTxnRef(String vnpayTxnRef);
    Optional<Payment> findFirstByAuctionIdOrderByCreatedAtDesc(Long auctionId);
}
