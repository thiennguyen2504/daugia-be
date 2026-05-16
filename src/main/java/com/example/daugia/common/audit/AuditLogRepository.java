package com.example.daugia.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, String>, JpaSpecificationExecutor<AuditLog> {
    Page<AuditLog> findAllByActorOrderByCreatedAtDesc(String actor, Pageable pageable);

    Page<AuditLog> findAllByActionAndCreatedAtBetween(AuditAction action, LocalDateTime from, LocalDateTime to, Pageable pageable);
}