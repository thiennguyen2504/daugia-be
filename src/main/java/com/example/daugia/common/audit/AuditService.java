package com.example.daugia.common.audit;

import com.example.daugia.common.dto.PageResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

public interface AuditService {
    void log(String actor, AuditAction action, String entityType, String entityId,
             AuditOutcome outcome, HttpServletRequest request, String detail);

    void log(String actor, AuditAction action, String entityType, String entityId,
             AuditOutcome outcome, String detail);

    PageResponse<AuditLogResponse> search(String actor, AuditAction action,
                                          LocalDateTime from, LocalDateTime to,
                                          int page, int size);
}