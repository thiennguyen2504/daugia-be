package com.example.daugia.mapper;

import com.example.daugia.dto.UserDto;
import com.example.daugia.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
}
