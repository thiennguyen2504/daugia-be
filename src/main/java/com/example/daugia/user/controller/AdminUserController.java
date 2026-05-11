package com.example.daugia.user.controller;

import com.example.daugia.common.dto.ApiResponse;
import com.example.daugia.common.dto.PageResponse;
import com.example.daugia.user.dto.UserAccountActionRequest;
import com.example.daugia.user.dto.UserAccountLogDto;
import com.example.daugia.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Users", description = "Admin user account management")
public class AdminUserController {

    private final UserService userService;

    @PutMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<Void>> lockUser(
            @PathVariable Long id,
            @RequestBody @Valid UserAccountActionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        userService.lockUser(id, jwt.getSubject(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success("User locked", null));
    }

    @PutMapping("/{id}/unlock")
    public ResponseEntity<ApiResponse<Void>> unlockUser(
            @PathVariable Long id,
            @RequestBody @Valid UserAccountActionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        userService.unlockUser(id, jwt.getSubject(), request.getReason());
        return ResponseEntity.ok(ApiResponse.success("User unlocked", null));
    }

    @GetMapping("/{id}/account-logs")
    public ResponseEntity<ApiResponse<PageResponse<UserAccountLogDto>>> getAccountLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Account logs fetched", userService.getAccountLogs(id, page, size)));
    }
}
