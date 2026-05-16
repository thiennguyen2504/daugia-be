package com.example.daugia.auction.repository;

import com.example.daugia.auction.entity.AuctionImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuctionImageRepository extends JpaRepository<AuctionImage, String> {
    List<AuctionImage> findAllByAuction_IdOrderBySortOrderAsc(String auctionId);
    Optional<AuctionImage> findByIdAndAuction_Id(String imageId, String auctionId);
    int countByAuction_Id(String auctionId);
}
