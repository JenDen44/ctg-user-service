package com.ctg.common;

import com.ctg.dto.PagedResponse;
import com.ctg.dto.UserRequest;
import com.ctg.dto.UserResponse;
import com.ctg.model.Role;
import com.ctg.model.User;

import java.util.List;

public class TestUserFactory {

    public static final Long USER_ID = 1L;

    public static UserRequest createUserRequest() {
        return UserRequest.builder()
                .email("employee@mail.com")
                .fullName("Employee Test")
                .password("EmployeeTestPassword")
                .role(Role.EMPLOYEE)
                .build();
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

    public static PagedResponse<UserResponse> pagedResponse() {
        return new PagedResponse<>(
                List.of(createUserResponse(USER_ID)), 0, 10, 1, 1
        );
    }

    public static UserResponse createUserResponse(Long userId) {
        return UserResponse.builder()
                .id(userId)
                .email("employee@mail.com")
                .fullName("Employee Test")
                .role(Role.EMPLOYEE)
                .build();
    }

    public static UserResponse createUserResponse(Long userId, UserRequest request) {
        return UserResponse.builder()
                .id(userId)
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(request.getRole())
                .build();
    }

    public static UserRequest createUserRequestWithEmail(String email) {
        return UserRequest.builder()
                .email(email)
                .fullName("Employee Test With Email")
                .password("testUserWithemail")
                .role(Role.ADMIN)
                .build();
    }

    public static UserRequest createUserRequestWithPassword(String password) {
        return UserRequest.builder()
                .email("testPassword@email.com")
                .fullName("Employee Test With Password")
                .password(password)
                .role(Role.ADMIN)
                .build();
    }

    public static UserRequest createUserRequestWithParams(String password, String email, String fullName) {
        return UserRequest.builder()
                .email(email)
                .fullName(fullName)
                .password(password)
                .role(Role.ADMIN)
                .build();
    }
}
