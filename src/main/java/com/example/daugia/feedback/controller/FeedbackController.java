package com.example.daugia.feedback.controller;

import com.example.daugia.common.dto.ApiResponse;
import com.example.daugia.common.dto.PageResponse;
import com.example.daugia.feedback.dto.FeedbackCreateRequest;
import com.example.daugia.feedback.dto.FeedbackReplyRequest;
import com.example.daugia.feedback.dto.FeedbackResponse;
import com.example.daugia.feedback.entity.FeedbackStatus;
import com.example.daugia.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "Feedback submission and admin review")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/api/v1/feedback")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FeedbackResponse>> submit(
            @Valid @RequestBody FeedbackCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Feedback submitted",
                        feedbackService.submit(request, jwt != null ? jwt.getSubject() : null)));
    }

    @GetMapping("/api/v1/admin/feedback")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<FeedbackResponse>>> getAll(
            @RequestParam(required = false) FeedbackStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Feedback fetched",
                feedbackService.getAll(status, page, size)));
    }

    @GetMapping("/api/v1/admin/feedback/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FeedbackResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Feedback fetched", feedbackService.getById(id)));
    }

    @PutMapping("/api/v1/admin/feedback/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FeedbackResponse>> resolve(
            @PathVariable String id,
            @Valid @RequestBody FeedbackReplyRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Feedback resolved",
                feedbackService.resolve(id, request, jwt != null ? jwt.getSubject() : null)));
    }

    @PutMapping("/api/v1/admin/feedback/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FeedbackResponse>> reject(
            @PathVariable String id,
            @Valid @RequestBody FeedbackReplyRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Feedback rejected",
                feedbackService.reject(id, request, jwt != null ? jwt.getSubject() : null)));
    }
}