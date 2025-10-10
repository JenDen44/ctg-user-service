package com.ctg.service;

import com.ctg.dto.LoginUserResponse;
import com.ctg.dto.PagedResponse;
import com.ctg.dto.UserRequest;
import com.ctg.dto.UserResponse;
import org.springframework.security.oauth2.jwt.Jwt;

public interface UserService {
    UserResponse get(Long id);
    UserResponse create(UserRequest newUser);
    UserResponse update(Long id, UserRequest updatedUser);
    void delete(Long id);
    PagedResponse<UserResponse> getByPage(int pageNo, int pageSize, String sortBy, String sortDir);
    LoginUserResponse findByEmailForLogin(String email);
    void incrementTokenVersion(Long id);
    UserResponse getByEmail(String email);
    UserResponse getCurrent(Jwt jwt);
}
