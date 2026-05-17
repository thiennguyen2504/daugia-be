package com.example.daugia.user.service;

import com.example.daugia.common.dto.PageResponse;
import com.example.daugia.user.dto.UserDto;
import com.example.daugia.user.dto.UserAccountLogDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UserService {
    PageResponse<UserDto> getAllUsers(int page, int size);
    UserDto findUserById(String id);
    String resolveUserId(String email);
    void lockUser(String targetUserId, String adminEmail, String reason);
    void unlockUser(String targetUserId, String adminEmail, String reason);
    PageResponse<UserAccountLogDto> getAccountLogs(String userId, int page, int size);
    UserDto updateProfile(String email, String fullName, String phone,
                          String street, String ward, String province,
                          MultipartFile avatar) throws IOException;

}
