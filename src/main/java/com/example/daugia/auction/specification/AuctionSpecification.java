package com.example.daugia.auction.specification;

import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class AuctionSpecification {

    private AuctionSpecification() {
    }

    public static Specification<Auction> publicVisible() {
        return (root, query, cb) -> root.get("status").in(List.of(
                AuctionStatus.APPROVED, AuctionStatus.ACTIVE, AuctionStatus.ENDED));
    }

    public static Specification<Auction> withSearch(String search) {
        return (root, query, cb) -> search == null || search.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("productName")), "%" + search.toLowerCase() + "%");
    }

    public static Specification<Auction> withCategory(String categoryId) {
        return (root, query, cb) -> categoryId == null
                ? cb.conjunction()
                : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Auction> withMinPrice(BigDecimal minPrice) {
        return (root, query, cb) -> minPrice == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("startingPrice"), minPrice);
    }

    public static Specification<Auction> withMaxPrice(BigDecimal maxPrice) {
        return (root, query, cb) -> maxPrice == null
                ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get("startingPrice"), maxPrice);
    }

    public static Specification<Auction> withStartFrom(LocalDateTime startFrom) {
        return (root, query, cb) -> startFrom == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("biddingStartTime"), startFrom);
    }

    public static Specification<Auction> withStartTo(LocalDateTime startTo) {
        return (root, query, cb) -> startTo == null
                ? cb.conjunction()
                : cb.lessThanOrEqualTo(root.get("biddingStartTime"), startTo);
    }

    public static Specification<Auction> withStatus(AuctionStatus status) {
        return (root, query, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }
}
