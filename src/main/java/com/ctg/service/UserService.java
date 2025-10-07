package com.ctg.service;

import com.ctg.dto.PagedResponse;
import com.ctg.dto.UserRequest;
import com.ctg.dto.UserResponse;
import com.ctg.model.User;

public interface UserService {
    UserResponse get(Long id);
    UserResponse create(UserRequest newUser);
    UserResponse update(Long id, UserRequest updatedUser);
    void delete(Long id);
    PagedResponse<UserResponse> getByPage(int pageNo, int pageSize, String sortBy, String sortDir);
    User findByEmail(String email);
    void incrementTokenVersion(Long id);
}
