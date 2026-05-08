package com.example.daugia.bidding.controller;

import com.example.daugia.bidding.dto.BidResponse;
import com.example.daugia.bidding.dto.LeaderboardEntryResponse;
import com.example.daugia.bidding.dto.PlaceBidRequest;
import com.example.daugia.bidding.entity.BidType;
import com.example.daugia.bidding.service.BiddingService;
import com.example.daugia.bidding.service.LeaderboardService;
import com.example.daugia.common.dto.ApiResponse;
import com.example.daugia.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auctions/{auctionId}")
@RequiredArgsConstructor
@Tag(name = "Bidding", description = "Live auction bidding")
public class BidRestController {

    private final BiddingService biddingService;
    private final LeaderboardService leaderboardService;

    @PostMapping("/bids")
    @PreAuthorize("hasRole('BIDDER')")
    @Operation(summary = "Place a bid through REST")
    public ResponseEntity<ApiResponse<BidResponse>> placeBid(@PathVariable Long auctionId,
                                                             @RequestBody @Valid PlaceBidRequest request,
                                                             @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Bid processed",
                biddingService.placeBid(auctionId, jwt.getSubject(), request.getAmount(), BidType.MANUAL)));
    }

    @GetMapping("/bids")
    @Operation(summary = "Get public bid history")
    public ResponseEntity<ApiResponse<PageResponse<BidResponse>>> history(@PathVariable Long auctionId,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "bidTime"));
        return ResponseEntity.ok(ApiResponse.success("Bid history fetched",
                PageResponse.from(biddingService.getBidHistory(auctionId, pageable))));
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Get public leaderboard")
    public ResponseEntity<ApiResponse<List<LeaderboardEntryResponse>>> leaderboard(@PathVariable Long auctionId) {
        return ResponseEntity.ok(ApiResponse.success("Leaderboard fetched", leaderboardService.getTop(auctionId)));
    }
}
