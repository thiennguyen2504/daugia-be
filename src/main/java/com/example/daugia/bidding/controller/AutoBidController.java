package com.example.daugia.bidding.controller;

import com.example.daugia.bidding.dto.AutoBidConfigRequest;
import com.example.daugia.bidding.dto.AutoBidConfigResponse;
import com.example.daugia.bidding.service.AutoBidService;
import com.example.daugia.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auctions/{auctionId}/auto-bid")
@RequiredArgsConstructor
@Tag(name = "Auto Bid", description = "Proxy bidding configuration")
public class AutoBidController {

    private final AutoBidService autoBidService;

    @PostMapping
    @PreAuthorize("hasRole('BIDDER')")
    @Operation(summary = "Create auto-bid config")
    public ResponseEntity<ApiResponse<AutoBidConfigResponse>> create(@PathVariable String auctionId,
                                                                     @RequestBody @Valid AutoBidConfigRequest request,
                                                                     @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Auto-bid configured",
                autoBidService.upsertConfig(auctionId, jwt.getSubject(), request.getMaxAmount())));
    }

    @PutMapping
    @PreAuthorize("hasRole('BIDDER')")
    @Operation(summary = "Update auto-bid config")
    public ResponseEntity<ApiResponse<AutoBidConfigResponse>> update(@PathVariable String auctionId,
                                                                     @RequestBody @Valid AutoBidConfigRequest request,
                                                                     @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Auto-bid updated",
                autoBidService.upsertConfig(auctionId, jwt.getSubject(), request.getMaxAmount())));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('BIDDER')")
    @Operation(summary = "Deactivate auto-bid config")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String auctionId,
                                                    @AuthenticationPrincipal Jwt jwt) {
        autoBidService.deactivateConfig(auctionId, jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success("Auto-bid deactivated", null));
    }

    @GetMapping
    @PreAuthorize("hasRole('BIDDER')")
    @Operation(summary = "Get own auto-bid config")
    public ResponseEntity<ApiResponse<AutoBidConfigResponse>> own(@PathVariable String auctionId,
                                                                  @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Auto-bid fetched",
                autoBidService.getOwnConfig(auctionId, jwt.getSubject())));
    }
}
