package com.ctg.service;

import com.ctg.dto.PagedResponse;
import com.ctg.dto.UserDto;

public interface UserService {
    UserDto getUser(Long id);
    UserDto createUser(UserDto userDto);
    UserDto updateUser(UserDto userDto, Long id);
    void deleteUser(Long id);
    PagedResponse<UserDto> getPagedUsers(int pageNo, int pageSize, String sortBy, String sortDir);
}
