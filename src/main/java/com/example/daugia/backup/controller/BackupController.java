package com.example.daugia.backup.controller;

import com.example.daugia.backup.dto.BackupResponse;
import com.example.daugia.backup.dto.BackupStatusResponse;
import com.example.daugia.backup.dto.RestoreRequest;
import com.example.daugia.backup.dto.RestoreResponse;
import com.example.daugia.backup.dto.RestoreResult;
import com.example.daugia.backup.entity.BackupRecord;
import com.example.daugia.backup.entity.BackupStatus;
import com.example.daugia.backup.entity.BackupType;
import com.example.daugia.backup.service.BackupService;
import com.example.daugia.backup.util.SizeFormatter;
import com.example.daugia.common.dto.ApiResponse;
import com.example.daugia.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin/backups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@ConditionalOnProperty(name = "backup.enabled", havingValue = "true")
@Tag(name = "Admin — Backup", description = "Backup and restore management")
public class BackupController {

    private final BackupService backupService;

    @PostMapping("/trigger")
    @Operation(summary = "Trigger full backup")
    public ResponseEntity<ApiResponse<BackupResponse>> triggerBackup(@AuthenticationPrincipal Jwt jwt) {
        BackupRecord record = backupService.triggerFullBackup(resolveActor(jwt));
        return ResponseEntity.accepted().body(ApiResponse.success("Backup scheduled", toResponse(record)));
    }

    @GetMapping
    @Operation(summary = "List backups")
    public ResponseEntity<ApiResponse<PageResponse<BackupResponse>>> listBackups(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) BackupType type,
            @RequestParam(required = false) BackupStatus status) {
        Page<BackupResponse> response = backupService.listBackups(pageable, type, status).map(this::toResponse);
        return ResponseEntity.ok(ApiResponse.success("Backups fetched", PageResponse.from(response)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get backup details")
    public ResponseEntity<ApiResponse<BackupResponse>> getBackup(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Backup fetched", toResponse(backupService.getBackup(id))));
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore from backup")
    public ResponseEntity<ApiResponse<RestoreResponse>> restore(@PathVariable String id,
                                                                @AuthenticationPrincipal Jwt jwt) {
        RestoreResult result = backupService.restoreFromBackup(id, resolveActor(jwt));
        return ResponseEntity.accepted().body(ApiResponse.success("Restore scheduled", toResponse(result)));
    }

    @PostMapping("/pitr")
    @Operation(summary = "Point-in-time restore")
    public ResponseEntity<ApiResponse<RestoreResponse>> pointInTimeRestore(
            @RequestBody RestoreRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        RestoreResult result = backupService.pointInTimeRestore(request.getTargetDateTime(), resolveActor(jwt));
        return ResponseEntity.accepted().body(ApiResponse.success("Point-in-time restore scheduled", toResponse(result)));
    }

    @GetMapping("/status")
    @Operation(summary = "Get backup status")
    public ResponseEntity<ApiResponse<BackupStatusResponse>> status() {
        return ResponseEntity.ok(ApiResponse.success("Backup status fetched", backupService.getStatus()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete backup")
    public ResponseEntity<ApiResponse<BackupResponse>> softDelete(@PathVariable String id,
                                                                  @AuthenticationPrincipal Jwt jwt) {
        BackupRecord record = backupService.softDelete(id, resolveActor(jwt));
        return ResponseEntity.ok(ApiResponse.success("Backup deleted", toResponse(record)));
    }

    private BackupResponse toResponse(BackupRecord record) {
        return BackupResponse.builder()
                .id(record.getId())
                .type(record.getType())
                .status(record.getStatus())
                .fileName(record.getFileName())
                .filePath(record.getFilePath())
                .fileSizeBytes(record.getFileSizeBytes())
                .fileSizeFormatted(SizeFormatter.formatBytes(record.getFileSizeBytes()))
                .durationMs(record.getDurationMs())
                .triggeredBy(record.getTriggeredBy())
                .errorMessage(record.getErrorMessage())
                .checksumSha256(record.getChecksumSha256())
                .createdAt(record.getCreatedAt())
                .completedAt(record.getCompletedAt())
                .build();
    }

    private RestoreResponse toResponse(RestoreResult result) {
        return RestoreResponse.builder()
                .restoreId(result.restoreId())
                .status(result.status())
                .message(result.message())
                .estimatedDurationMs(result.estimatedDurationMs())
                .build();
    }

    private String resolveActor(Jwt jwt) {
        return Optional.ofNullable(jwt).map(Jwt::getSubject).orElse("ANONYMOUS");
    }
}
