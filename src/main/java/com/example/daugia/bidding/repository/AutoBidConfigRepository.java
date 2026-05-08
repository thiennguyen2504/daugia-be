package com.example.daugia.bidding.repository;

import com.example.daugia.bidding.entity.AutoBidConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AutoBidConfigRepository extends JpaRepository<AutoBidConfig, Long> {
    Optional<AutoBidConfig> findByAuctionIdAndBidderEmail(Long auctionId, String bidderEmail);

    List<AutoBidConfig> findAllByAuctionIdAndActiveTrueAndBidderIdNotAndMaxAmountGreaterThanOrderByMaxAmountDesc(
            Long auctionId,
            Long bidderId,
            BigDecimal currentPrice);
}
