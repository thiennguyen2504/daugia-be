package com.example.daugia.user.dto;

import com.example.daugia.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private com.example.daugia.user.entity.Role role;
    private boolean enabled;
}
