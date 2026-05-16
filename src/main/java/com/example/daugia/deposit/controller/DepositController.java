package com.example.daugia.deposit.controller;

import com.example.daugia.common.dto.ApiResponse;
import com.example.daugia.deposit.dto.DepositResponse;
import com.example.daugia.deposit.mapper.DepositMapper;
import com.example.daugia.deposit.service.DepositService;
import com.example.daugia.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auctions/{auctionId}/deposit")
@RequiredArgsConstructor
@Tag(name = "Deposit", description = "Auction deposit and escrow")
public class DepositController {

    private final DepositService depositService;
    private final UserService userService;
    private final DepositMapper depositMapper;

    @PostMapping
    @PreAuthorize("hasRole('BIDDER')")
    @Operation(summary = "Hold bidder deposit for an auction")
    public ResponseEntity<ApiResponse<DepositResponse>> holdDeposit(@PathVariable Long auctionId,
                                                            @AuthenticationPrincipal Jwt jwt) {
        Long bidderId = userService.resolveUserId(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success("Deposit held",
                depositMapper.toResponse(depositService.holdDeposit(auctionId, bidderId, depositService.getDepositAmount(auctionId)))));
    }
}
