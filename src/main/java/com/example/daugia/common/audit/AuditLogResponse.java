package com.example.daugia.common.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogResponse {
    private String id;
    private String actor;
    private AuditAction action;
    private String entityType;
    private String entityId;
    private AuditOutcome outcome;
    private String ipAddress;
    private String userAgent;
    private String detail;
    private String requestId;
    private LocalDateTime createdAt;
}