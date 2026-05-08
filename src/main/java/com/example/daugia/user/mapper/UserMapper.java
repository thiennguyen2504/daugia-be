package com.example.daugia.user.mapper;

import com.example.daugia.user.dto.UserDto;
import com.example.daugia.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "fullName", source = "fullName")
    UserDto toDto(User user);
}
