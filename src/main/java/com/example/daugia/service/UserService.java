package com.example.daugia.service;

import com.example.daugia.dto.UserDto;
import java.util.List;

public interface UserService {
    List<UserDto> findAllUsers();
    UserDto findUserById(Long id);
}
