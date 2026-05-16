package com.example.daugia.deposit.mapper;

import com.example.daugia.deposit.dto.DepositResponse;
import com.example.daugia.deposit.entity.Deposit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DepositMapper {

    @Mapping(target = "auctionId", source = "auction.id")
    @Mapping(target = "productName", source = "auction.productName")
    @Mapping(target = "bidderId", source = "bidder.id")
    @Mapping(target = "bidderEmail", source = "bidder.email")
    DepositResponse toResponse(Deposit deposit);
}
