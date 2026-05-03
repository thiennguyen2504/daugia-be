package com.example.daugia.user.mapper;

import com.example.daugia.user.dto.UserDto;
import com.example.daugia.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
}
