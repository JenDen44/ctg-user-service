package com.ctg.service;

import com.ctg.dto.PagedResponse;
import com.ctg.dto.UserRequest;
import com.ctg.dto.UserResponse;

public interface UserService {
    UserResponse getUser(Long id);
    UserResponse createUser(UserRequest newUser);
    UserResponse updateUser(UserRequest updatedUser, Long id);
    void deleteUser(Long id);
    PagedResponse<UserResponse> getPagedUsers(int pageNo, int pageSize, String sortBy, String sortDir);
}
