package com.ctg.common;

import com.ctg.dto.PagedResponse;
import com.ctg.dto.UserDto;
import com.ctg.model.Role;
import com.ctg.model.User;

import java.util.List;

public class TestUserFactory {

    public static final Long USER_ID = 1L;

    public static UserDto createUserDto(Long id) {
        return UserDto.builder()
                .id(id)
                .email("employee@mail.com")
                .fullName("Employee Test")
                .password("EmployeeTestPassword")
                .role(Role.EMPLOYEE)
                .build();
    }

    public static UserDto createUserDto() {
        return createUserDto(null);
    }

    public static User createUser(Long id) {
        return User.builder()
                .id(id)
                .email("employee@mail.com")
                .fullName("Employee Test")
                .password("EmployeeTestPassword")
                .role(Role.EMPLOYEE)
                .build();
    }

    public static User createUser() {
        return createUser(null);
    }

    public static UserDto userDtoWithEmail(String email) {
        return UserDto.builder()
                .email(email)
                .fullName("Employee Test")
                .password("ValidPass123")
                .role(Role.EMPLOYEE)
                .build();
    }

    public static UserDto userDtoWithPassword(String password) {
        return UserDto.builder()
                .email("employee@mail.com")
                .fullName("Employee Test")
                .password(password)
                .role(Role.EMPLOYEE)
                .build();
    }

    public static UserDto userDtoWithIdAndPassword(Long userId, String password) {
        return UserDto.builder()
                .id(userId)
                .email("employee@mail.com")
                .fullName("Employee Test")
                .password(password)
                .role(Role.EMPLOYEE)
                .build();
    }

    public static PagedResponse<UserDto> pagedResponse() {
        return new PagedResponse<>(
                List.of(createUserDto(USER_ID)), 0, 10, 1, 1
        );
    }
}