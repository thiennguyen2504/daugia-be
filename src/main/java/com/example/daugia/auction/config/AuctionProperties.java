package com.example.daugia.auction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties("auction")
public record AuctionProperties(
        Deposit deposit,
        Antisnipe antinsiping
) {
    public AuctionProperties {
        deposit = deposit == null ? new Deposit(new BigDecimal("0.10")) : deposit;
        antinsiping = antinsiping == null ? new Antisnipe(60, 120) : antinsiping;
    }

    public record Deposit(BigDecimal ratio) {
        public Deposit {
            ratio = ratio == null ? new BigDecimal("0.10") : ratio;
        }
    }

    public record Antisnipe(long windowSeconds, long extensionSeconds) {
        public Antisnipe {
            windowSeconds = windowSeconds <= 0 ? 60 : windowSeconds;
            extensionSeconds = extensionSeconds <= 0 ? 120 : extensionSeconds;
        }
    }
}
