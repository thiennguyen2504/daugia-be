package com.example.daugia.auction.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;  

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuctionCreateRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 255)
    private String productName;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Starting price is required")
    @DecimalMin(value = "0.01", message = "Starting price must be greater than 0")
    private BigDecimal startingPrice;

    @NotNull(message = "Bid increment is required")
    @DecimalMin(value = "0.01", message = "Bid increment must be greater than 0")
    private BigDecimal bidIncrement;

    @DecimalMin(value = "0.01", message = "Buy now price must be greater than 0")
    private BigDecimal buyNowPrice;

    @NotNull(message = "Category is required")
    private String categoryId;

    @NotNull(message = "Bidding start time is required")
    @Future(message = "Bidding start time must be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime biddingStartTime;

    @NotNull(message = "Bidding end time is required")
    @Future(message = "Bidding end time must be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime biddingEndTime;
}
