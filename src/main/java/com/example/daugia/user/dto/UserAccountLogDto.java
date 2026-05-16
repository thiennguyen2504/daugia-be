package com.example.daugia.user.dto;

import com.example.daugia.user.entity.UserAccountAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountLogDto {
    private String id;
    private String targetUserId;
    private String targetUserEmail;
    private String performedBy;
    private UserAccountAction action;
    private String reason;
    private LocalDateTime createdAt;
}
