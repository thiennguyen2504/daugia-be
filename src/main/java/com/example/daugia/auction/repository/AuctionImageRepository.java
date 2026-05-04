package com.example.daugia.auction.repository;

import com.example.daugia.auction.entity.AuctionImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuctionImageRepository extends JpaRepository<AuctionImage, Long> {
    List<AuctionImage> findAllByAuction_IdOrderBySortOrderAsc(Long auctionId);
    Optional<AuctionImage> findByIdAndAuction_Id(Long imageId, Long auctionId);
    int countByAuction_Id(Long auctionId);
}
