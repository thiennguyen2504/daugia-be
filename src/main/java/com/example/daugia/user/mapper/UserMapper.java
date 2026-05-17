package com.example.daugia.user.mapper;

import com.example.daugia.user.dto.UserDto;
import com.example.daugia.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "fullName",  source = "fullName")
    @Mapping(target = "role",      expression = "java(user.getRole() != null ? user.getRole().getName() : null)")
    @Mapping(target = "street",    source = "street")
    @Mapping(target = "ward",      source = "ward")
    @Mapping(target = "province",  source = "province")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    UserDto toDto(User user);
}
