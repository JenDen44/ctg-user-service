package com.ctg.mapper;

import com.ctg.dto.UserDto;
import com.ctg.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    @Mapping(target = "password", ignore = true)
    UserDto toDto(User user);

    @Mapping(target = "id", ignore = true)
    User toEntity(UserDto userDto);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(UserDto userDto, @MappingTarget User existingUser);
}
