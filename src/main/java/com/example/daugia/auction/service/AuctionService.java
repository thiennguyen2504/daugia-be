package com.example.daugia.auction.service;

import com.example.daugia.auction.dto.*;
import com.example.daugia.common.dto.PageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface AuctionService {

    // Seller
    AuctionResponse create(AuctionCreateRequest request, String sellerEmail);
    PageResponse<AuctionSummaryResponse> getMyAuctions(String sellerEmail, int page, int size);

    // Public / Bidder
    PageResponse<AuctionSummaryResponse> searchPublic(AuctionFilterRequest filter, int page, int size);
    AuctionResponse getById(Long id, String currentUserEmail, boolean isAdmin);

    // Admin
    PageResponse<AuctionSummaryResponse> searchAdmin(AuctionFilterRequest filter, int page, int size);
    AuctionResponse review(Long id, AuctionReviewRequest request, String adminEmail);

    // Image upload (Seller)
    AuctionImageResponse uploadImage(Long auctionId, MultipartFile file, String sellerEmail) throws IOException;
}
