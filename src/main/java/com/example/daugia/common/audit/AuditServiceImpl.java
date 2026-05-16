package com.example.daugia.common.audit;

import com.example.daugia.common.dto.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void log(String actor, AuditAction action, String entityType, String entityId,
                    AuditOutcome outcome, HttpServletRequest request, String detail) {
        String resolvedActor = normalizeActor(actor);
        String requestId = resolveRequestId(request);
        String ipAddress = resolveIpAddress(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        auditLogRepository.save(AuditLog.builder()
                .actor(resolvedActor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .outcome(outcome)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .detail(detail)
                .requestId(requestId)
                .build());

        log.info("AUDIT action={} actor={} entity={}/{} outcome={} ip={} requestId={} detail={}",
                action, resolvedActor, entityType, entityId, outcome, ipAddress, requestId, detail);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void log(String actor, AuditAction action, String entityType, String entityId,
                    AuditOutcome outcome, String detail) {
        String resolvedActor = normalizeActor(actor);
        String requestId = MDC.get("requestId");

        auditLogRepository.save(AuditLog.builder()
                .actor(resolvedActor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .outcome(outcome)
                .detail(detail)
                .requestId(requestId)
                .build());

        log.info("AUDIT action={} actor={} entity={}/{} outcome={} ip={} requestId={} detail={}",
                action, resolvedActor, entityType, entityId, outcome, null, requestId, detail);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(String actor, AuditAction action,
                                                 LocalDateTime from, LocalDateTime to,
                                                 int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<AuditLog> specification = Specification.allOf();
        if (actor != null && !actor.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("actor"), actor));
        }
        if (action != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("action"), action));
        }
        if (from != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        return PageResponse.from(auditLogRepository.findAll(specification, pageable).map(this::toResponse));
    }

    private AuditLogResponse toResponse(AuditLog logEntry) {
        return AuditLogResponse.builder()
                .id(logEntry.getId())
                .actor(logEntry.getActor())
                .action(logEntry.getAction())
                .entityType(logEntry.getEntityType())
                .entityId(logEntry.getEntityId())
                .outcome(logEntry.getOutcome())
                .ipAddress(logEntry.getIpAddress())
                .userAgent(logEntry.getUserAgent())
                .detail(logEntry.getDetail())
                .requestId(logEntry.getRequestId())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }

    private String resolveRequestId(HttpServletRequest request) {
        if (request != null) {
            Object requestId = request.getAttribute("requestId");
            if (requestId != null) {
                return requestId.toString();
            }
        }
        return MDC.get("requestId");
    }

    private String resolveIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String normalizeActor(String actor) {
        return (actor == null || actor.isBlank()) ? "ANONYMOUS" : actor;
    }
}