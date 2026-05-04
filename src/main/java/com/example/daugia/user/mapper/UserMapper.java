package com.example.daugia.user.mapper;

import com.example.daugia.user.dto.UserDto;
import com.example.daugia.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @org.mapstruct.Mapping(target = "fullName", expression = "java(user.getFirstname() + \" \" + user.getLastname())")
    UserDto toDto(User user);
}
