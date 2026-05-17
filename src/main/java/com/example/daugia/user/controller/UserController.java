package com.example.daugia.user.controller;

import com.example.daugia.common.dto.ApiResponse;
import com.example.daugia.common.dto.PageResponse;
import com.example.daugia.user.dto.UserDto;
import com.example.daugia.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Fetched users successfully", userService.getAllUsers(page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Fetched user successfully", userService.findUserById(id)));
    }

    @PutMapping(value = "/profile", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            @RequestParam(value = "fullName",  required = false) String fullName,
            @RequestParam(value = "phone",     required = false) String phone,
            @RequestParam(value = "street",    required = false) String street,
            @RequestParam(value = "ward",      required = false) String ward,
            @RequestParam(value = "province",  required = false) String province,
            @RequestParam(value = "avatar",    required = false) MultipartFile avatar,
            @AuthenticationPrincipal Jwt jwt) throws IOException {
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully",
                userService.updateProfile(jwt.getSubject(), fullName, phone, street, ward, province, avatar)));
    }

}
