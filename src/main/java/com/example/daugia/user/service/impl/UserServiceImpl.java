package com.example.daugia.user.service.impl;

import com.example.daugia.common.dto.PageResponse;
import com.example.daugia.common.audit.AuditAction;
import com.example.daugia.common.audit.AuditJsonUtils;
import com.example.daugia.common.audit.AuditOutcome;
import com.example.daugia.common.audit.AuditService;
import com.example.daugia.common.exception.AppException;
import com.example.daugia.user.dto.UserDto;
import com.example.daugia.user.dto.UserAccountLogDto;
import com.example.daugia.common.exception.ResourceNotFoundException;
import com.example.daugia.user.entity.UserAccountAction;
import com.example.daugia.user.entity.UserAccountLog;
import com.example.daugia.user.mapper.UserMapper;
import com.example.daugia.user.repository.UserAccountLogRepository;
import com.example.daugia.user.repository.UserRepository;
import com.example.daugia.user.service.UserService;
import com.example.daugia.user.util.UserNameUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final UserAccountLogRepository userAccountLogRepository;
    private final UserMapper userMapper;
    private final com.example.daugia.common.storage.StorageService storageService;
    private final AuditService auditService;

    @Override
    public PageResponse<UserDto> getAllUsers(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return PageResponse.from(repository.findAll(pageable).map(userMapper::toDto));
    }

    @Override
    public UserDto findUserById(String id) {
        return repository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public String resolveUserId(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email))
                .getId();
    }

    @Override
    @Transactional
    public void lockUser(String targetUserId, String adminEmail, String reason) {
        var user = repository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getName())) {
            throw new AppException("Cannot lock an ADMIN account", HttpStatus.FORBIDDEN);
        }

        user.setLocked(true);
        repository.save(user);
        userAccountLogRepository.save(UserAccountLog.builder()
                .targetUser(user)
                .performedBy(adminEmail)
                .action(UserAccountAction.LOCK)
                .reason(reason)
                .build());
        auditService.log(adminEmail, AuditAction.USER_LOCKED, "USER", targetUserId,
                AuditOutcome.SUCCESS,
                AuditJsonUtils.toJson("reason", reason, "lockedBy", adminEmail));
    }

    @Override
    @Transactional
    public void unlockUser(String targetUserId, String adminEmail, String reason) {
        var user = repository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getName())) {
            throw new AppException("Cannot lock an ADMIN account", HttpStatus.FORBIDDEN);
        }

        user.setLocked(false);
        repository.save(user);
        userAccountLogRepository.save(UserAccountLog.builder()
                .targetUser(user)
                .performedBy(adminEmail)
                .action(UserAccountAction.UNLOCK)
                .reason(reason)
                .build());
        auditService.log(adminEmail, AuditAction.USER_UNLOCKED, "USER", targetUserId,
                AuditOutcome.SUCCESS,
                AuditJsonUtils.toJson("reason", reason, "unlockedBy", adminEmail));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserAccountLogDto> getAccountLogs(String userId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(userAccountLogRepository.findAllByTargetUser_Id(userId, pageable)
                .map(log -> UserAccountLogDto.builder()
                        .id(log.getId())
                        .targetUserId(log.getTargetUser().getId())
                        .targetUserEmail(log.getTargetUser().getEmail())
                        .performedBy(log.getPerformedBy())
                        .action(log.getAction())
                        .reason(log.getReason())
                        .createdAt(log.getCreatedAt())
                        .build()));
    }

    @Override
    @Transactional
    public UserDto updateProfile(String email, String fullName, String phone,
                                 String street, String ward, String province,
                                 org.springframework.web.multipart.MultipartFile avatar) throws java.io.IOException {
        com.example.daugia.user.entity.User user = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (fullName != null && !fullName.isBlank()) {
            String[] nameParts = UserNameUtils.splitFullName(fullName);
            user.setFirstname(nameParts[0]);
            user.setLastname(nameParts[1]);
        }

        if (phone != null && !phone.isBlank()) {
            user.setPhone(phone);
        }

        if (street != null) user.setStreet(street);
        if (ward != null)   user.setWard(ward);
        if (province != null) user.setProvince(province);

        if (avatar != null && !avatar.isEmpty()) {
            if (user.getAvatarPublicId() != null) {
                storageService.delete(user.getAvatarPublicId());
            }

            var result = storageService.upload(avatar.getBytes(), avatar.getOriginalFilename(), "avatars/" + user.getId());
            user.setAvatarUrl(result.url());
            user.setAvatarPublicId(result.publicId());
        }

        UserDto updated = userMapper.toDto(repository.save(user));
        auditService.log(email, AuditAction.PROFILE_UPDATED, "USER", user.getId(),
                AuditOutcome.SUCCESS,
                AuditJsonUtils.toJson("email", email));
        return updated;
    }

}
