package com.example.daugia.bidding.service;

import com.example.daugia.bidding.dto.BidResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisBidSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            BidResponse response = objectMapper.readValue(json, BidResponse.class);
            simpMessagingTemplate.convertAndSend("/topic/auctions/" + response.getAuctionId(), response);
        } catch (Exception ex) {
            log.warn("Could not process Redis bid event", ex);
        }
    }
}
