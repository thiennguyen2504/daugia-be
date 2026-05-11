package com.example.daugia.user.service;

import com.example.daugia.common.dto.PageResponse;
import com.example.daugia.user.dto.UserAccountLogDto;
import com.example.daugia.user.dto.UserDto;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    PageResponse<UserDto> getAllUsers(int page, int size);
    UserDto findUserById(Long id);
    Long resolveUserId(String email);
    UserDto updateProfile(String email, String fullName, String phone, MultipartFile avatar) throws java.io.IOException;

    void lockUser(Long targetUserId, String adminEmail, String reason);
    void unlockUser(Long targetUserId, String adminEmail, String reason);
    PageResponse<UserAccountLogDto> getAccountLogs(Long userId, int page, int size);
}
