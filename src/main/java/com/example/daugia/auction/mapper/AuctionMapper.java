package com.example.daugia.auction.mapper;

import com.example.daugia.auction.dto.AuctionImageResponse;
import com.example.daugia.auction.dto.AuctionResponse;
import com.example.daugia.auction.dto.AuctionSummaryResponse;
import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuctionMapper {

    @Mapping(target = "sellerId",     source = "seller.id")
    @Mapping(target = "sellerEmail",  source = "seller.email")
    @Mapping(target = "sellerName",   source = "seller.fullName")
    @Mapping(target = "categoryId",   source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "endTime", source = "endTime")
    @Mapping(target = "extensionCount", source = "extensionCount")
    @Mapping(target = "thumbnailUrl", expression = "java(auction.getImages().isEmpty() ? null : auction.getImages().get(0).getImageUrl())")
    AuctionResponse toResponse(Auction auction);

    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "endTime", source = "endTime")
    @Mapping(target = "thumbnailUrl", expression = "java(auction.getImages().isEmpty() ? null : auction.getImages().get(0).getImageUrl())")
    AuctionSummaryResponse toSummary(Auction auction);

    @Mapping(target = "uploadedAt", source = "createdAt")
    @Mapping(target = "updatedAt",  source = "updatedAt")
    AuctionImageResponse toImageResponse(AuctionImage image);
}

