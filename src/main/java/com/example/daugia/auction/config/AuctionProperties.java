package com.example.daugia.auction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("auction")
public record AuctionProperties(
        Antisnipe antinsiping
) {
    public AuctionProperties {
        antinsiping = antinsiping == null ? new Antisnipe(60, 120) : antinsiping;
    }

    public record Antisnipe(long windowSeconds, long extensionSeconds) {
        public Antisnipe {
            windowSeconds = windowSeconds <= 0 ? 60 : windowSeconds;
            extensionSeconds = extensionSeconds <= 0 ? 120 : extensionSeconds;
        }
    }
}
