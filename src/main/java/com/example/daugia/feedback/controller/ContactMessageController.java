package com.example.daugia.feedback.controller;

import com.example.daugia.common.dto.ApiResponse;
import com.example.daugia.common.dto.PageResponse;
import com.example.daugia.feedback.dto.ContactMessageCreateRequest;
import com.example.daugia.feedback.dto.ContactMessageReplyRequest;
import com.example.daugia.feedback.dto.ContactMessageResponse;
import com.example.daugia.feedback.entity.ContactStatus;
import com.example.daugia.feedback.service.ContactMessageService;
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
@Tag(name = "Contact Message", description = "Public contact message submission and admin review")
public class ContactMessageController {

    private final ContactMessageService contactMessageService;

    @PostMapping("/api/v1/contact")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> submit(@Valid @RequestBody ContactMessageCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Contact message submitted", contactMessageService.submit(request)));
    }

    @GetMapping("/api/v1/admin/contact")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ContactMessageResponse>>> getAll(
            @RequestParam(required = false) ContactStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Contact messages fetched",
                contactMessageService.getAll(status, page, size)));
    }

    @GetMapping("/api/v1/admin/contact/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Contact message fetched", contactMessageService.getById(id)));
    }

    @PutMapping("/api/v1/admin/contact/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> resolve(
            @PathVariable String id,
            @Valid @RequestBody ContactMessageReplyRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Contact message resolved",
                contactMessageService.resolve(id, request, jwt != null ? jwt.getSubject() : null)));
    }

    @PutMapping("/api/v1/admin/contact/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> reject(
            @PathVariable String id,
            @Valid @RequestBody ContactMessageReplyRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Contact message rejected",
                contactMessageService.reject(id, request, jwt != null ? jwt.getSubject() : null)));
    }
}