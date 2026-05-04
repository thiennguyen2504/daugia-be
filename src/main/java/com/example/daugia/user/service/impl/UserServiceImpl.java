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
    private final com.example.daugia.common.storage.StorageService storageService;

    @Override
    public List<UserDto> findAllUsers() {
        return repository.findAllWithRole().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto findUserById(Long id) {
        return repository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public UserDto updateProfile(String email, String fullName, String phone, org.springframework.web.multipart.MultipartFile avatar) throws java.io.IOException {
        com.example.daugia.user.entity.User user = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (fullName != null && !fullName.isBlank()) {
            String[] nameParts = fullName.trim().split("\\s+");
            user.setFirstname(nameParts[0]);
            user.setLastname(nameParts.length > 1
                    ? String.join(" ", java.util.Arrays.copyOfRange(nameParts, 1, nameParts.length))
                    : "");
        }

        if (phone != null && !phone.isBlank()) {
            user.setPhone(phone);
        }

        if (avatar != null && !avatar.isEmpty()) {
            // Delete old avatar if exists
            if (user.getAvatarPublicId() != null) {
                storageService.delete(user.getAvatarPublicId());
            }

            var result = storageService.upload(avatar.getBytes(), avatar.getOriginalFilename(), "avatars/" + user.getId());
            user.setAvatarUrl(result.url());
            user.setAvatarPublicId(result.publicId());
        }

        return userMapper.toDto(repository.save(user));
    }
}
