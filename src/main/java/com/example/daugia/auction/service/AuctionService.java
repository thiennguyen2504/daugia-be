package com.example.daugia.auction.service;

import com.example.daugia.auction.dto.*;
import com.example.daugia.common.dto.PageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


public interface AuctionService {

    // Seller
    AuctionResponse create(AuctionCreateRequest request, List<MultipartFile> images, String sellerEmail) throws IOException;
    PageResponse<AuctionSummaryResponse> getMyAuctions(String sellerEmail, int page, int size);

    // Public / Bidder
    PageResponse<AuctionSummaryResponse> searchPublic(AuctionFilterRequest filter, int page, int size);
    AuctionResponse getById(String id, String currentUserEmail, boolean isAdmin);

    // Admin
    PageResponse<AuctionSummaryResponse> searchAdmin(AuctionFilterRequest filter, int page, int size);
    AuctionResponse review(String id, AuctionReviewRequest request, String adminEmail);

}
