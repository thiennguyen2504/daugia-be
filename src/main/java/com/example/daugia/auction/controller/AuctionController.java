package com.example.daugia.auction.controller;

import com.example.daugia.auction.dto.*;
import com.example.daugia.auction.service.AuctionService;
import com.example.daugia.common.dto.ApiResponse;
import com.example.daugia.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
@Tag(name = "Auction", description = "Auction listing and management")
public class AuctionController {

    private final AuctionService auctionService;

    // ─── PUBLIC / BIDDER ──────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuctionSummaryResponse>>> search(
            @RequestParam(defaultValue = "0")     int page,
            @RequestParam(defaultValue = "12")    int size,
            @RequestParam(required = false)       String search,
            @RequestParam(required = false)       Long categoryId,
            @RequestParam(required = false)       BigDecimal minPrice,
            @RequestParam(required = false)       BigDecimal maxPrice,
            @RequestParam(required = false)       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startFrom,
            @RequestParam(required = false)       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTo,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")  String sortDir) {

        AuctionFilterRequest filter = AuctionFilterRequest.builder()
                .search(search).categoryId(categoryId)
                .minPrice(minPrice).maxPrice(maxPrice)
                .startFrom(startFrom).startTo(startTo)
                .sortBy(sortBy).sortDir(sortDir)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Auctions fetched",
                auctionService.searchPublic(filter, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuctionResponse>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        String email = (jwt != null) ? jwt.getSubject() : null;
        return ResponseEntity.ok(ApiResponse.success("Auction fetched",
                auctionService.getById(id, email, false)));
    }

    // ─── SELLER ───────────────────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<AuctionResponse>> create(
            @RequestPart("request") @Valid AuctionCreateRequest request,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal Jwt jwt) throws IOException {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Auction submitted for review",
                        auctionService.create(request, files, jwt.getSubject())));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<PageResponse<AuctionSummaryResponse>>> getMyAuctions(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("My auctions fetched",
                auctionService.getMyAuctions(jwt.getSubject(), page, size)));
    }

}
