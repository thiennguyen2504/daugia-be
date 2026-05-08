package com.example.daugia.deposit.repository;

import com.example.daugia.deposit.entity.Deposit;
import com.example.daugia.deposit.entity.DepositStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepositRepository extends JpaRepository<Deposit, Long> {
    Optional<Deposit> findByAuctionIdAndBidderId(Long auctionId, Long bidderId);

    boolean existsByAuctionIdAndBidderIdAndStatus(Long auctionId, Long bidderId, DepositStatus status);

    List<Deposit> findAllByAuctionIdAndStatus(Long auctionId, DepositStatus status);
}
