package com.example.daugia.feedback.dto;

import com.example.daugia.feedback.entity.FeedbackStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {
    private String id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String content;
    private String response;
    private FeedbackStatus status;
    private String respondedBy;
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}