package com.example.daugia.user.service.impl;

import com.example.daugia.user.dto.UserDto;
import com.example.daugia.common.exception.ResourceNotFoundException;
import com.example.daugia.user.mapper.UserMapper;
import com.example.daugia.user.repository.UserRepository;
import com.example.daugia.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final UserMapper userMapper;

    @Override
    public List<UserDto> findAllUsers() {
        return repository.findAllWithRoles().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto findUserById(Long id) {
        return repository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
