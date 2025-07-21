package com.ctg.mapper;

import com.ctg.dto.UserDto;
import com.ctg.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
    User toEntity(UserDto userDto);
}
