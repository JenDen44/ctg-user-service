package com.ctg.service;

import com.ctg.dto.PagedResponse;
import com.ctg.dto.UserRequest;
import com.ctg.dto.UserResponse;
import com.ctg.model.User;

public interface UserService {
    UserResponse get(Long id);
    UserResponse create(UserRequest newUser);
    UserResponse update(UserRequest updatedUser, Long id);
    void delete(Long id);
    PagedResponse<UserResponse> getPage(int pageNo, int pageSize, String sortBy, String sortDir);
    User findByEmailForLogin(String email);
    void incrementTokenVersion(Long id);
}
