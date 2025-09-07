package com.ctg.mapper;

import com.ctg.dto.UserRequest;
import com.ctg.dto.UserResponse;
import com.ctg.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    UserResponse toDto(User user);

    @Mapping(target = "id", ignore = true)
    User toEntity(UserRequest userDto);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(UserRequest updatedUser, @MappingTarget User existingUser);
}
