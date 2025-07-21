package com.ctg.controller;

import com.ctg.constants.PaginationConstants;
import com.ctg.dto.PagedResponse;
import com.ctg.dto.UserDto;
import com.ctg.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @GetMapping
    public PagedResponse<UserDto> getUsers(
            @RequestParam(value = "pageNo",
                    defaultValue = PaginationConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value = "pageSize",
                    defaultValue = PaginationConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sortBy",
                    defaultValue = PaginationConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value = "sortDir",
                    defaultValue = PaginationConstants.DEFAULT_SORT_DIR, required = false) String sortDir
    ) {
       return userService.getPagedUsers(pageNo, pageSize, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable("id") Long id) {
        return userService.getUser(id);
    }

    @PostMapping
    public UserDto createNewUser(@RequestBody UserDto newUser) {
        return userService.createNewUser(newUser);
    }

    @PutMapping("/{id}")
    public UserDto updateUser(@RequestBody UserDto updatedUser, @PathVariable("id") Long id) {
        return userService.updateUser(updatedUser, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
    }

/*    @GetMapping("/current")
    public UserDto getCurrentUser() {
        подумать как реализовать
    }*/
}
