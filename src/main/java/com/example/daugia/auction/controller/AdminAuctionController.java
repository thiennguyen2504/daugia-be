package com.example.daugia.auction.controller;

import com.example.daugia.auction.dto.*;
import com.example.daugia.auction.entity.AuctionStatus;
import com.example.daugia.auction.service.AuctionService;
import com.example.daugia.common.dto.ApiResponse;
import com.example.daugia.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/auctions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Auction", description = "Admin auction review and management")
public class AdminAuctionController {

    private final AuctionService auctionService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuctionSummaryResponse>>> searchAll(
            @RequestParam(defaultValue = "0")     int page,
            @RequestParam(defaultValue = "12")    int size,
            @RequestParam(required = false)       String search,
            @RequestParam(required = false)       Long categoryId,
            @RequestParam(required = false)       AuctionStatus status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")  String sortDir) {

        AuctionFilterRequest filter = AuctionFilterRequest.builder()
                .search(search).categoryId(categoryId).status(status)
                .sortBy(sortBy).sortDir(sortDir)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Auctions fetched",
                auctionService.searchAdmin(filter, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuctionResponse>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Auction fetched",
                auctionService.getById(id, jwt.getSubject(), true)));
    }

    @PutMapping("/{id}/review")
    public ResponseEntity<ApiResponse<AuctionResponse>> review(
            @PathVariable Long id,
            @RequestBody @Valid AuctionReviewRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Auction reviewed",
                auctionService.review(id, request, jwt.getSubject())));
    }
}
