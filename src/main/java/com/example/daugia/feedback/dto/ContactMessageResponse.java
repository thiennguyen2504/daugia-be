package com.example.daugia.feedback.dto;

import com.example.daugia.feedback.entity.ContactStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessageResponse {
    private String id;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String message;
    private String response;
    private ContactStatus status;
    private String respondedBy;
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}