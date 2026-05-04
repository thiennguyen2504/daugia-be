package com.example.daugia.user.service;

import com.example.daugia.user.dto.UserDto;
import java.util.List;

public interface UserService {
    List<UserDto> findAllUsers();
    UserDto findUserById(Long id);
    UserDto updateProfile(String email, String fullName, String phone, org.springframework.web.multipart.MultipartFile avatar) throws java.io.IOException;
}
