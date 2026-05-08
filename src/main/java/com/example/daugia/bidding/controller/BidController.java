package com.example.daugia.bidding.controller;

import com.example.daugia.bidding.dto.BidResponse;
import com.example.daugia.bidding.dto.PlaceBidRequest;
import com.example.daugia.bidding.entity.BidType;
import com.example.daugia.bidding.service.BiddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class BidController {

    private final BiddingService biddingService;

    @MessageMapping("/auctions/{auctionId}/bid")
    @SendTo("/topic/auctions/{auctionId}")
    public BidResponse placeBid(@DestinationVariable Long auctionId,
                                PlaceBidRequest request,
                                Principal principal) {
        return biddingService.placeBid(auctionId, principal.getName(), request.getAmount(), BidType.MANUAL);
    }
}
