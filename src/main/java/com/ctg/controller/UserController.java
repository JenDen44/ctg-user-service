package com.ctg.controller;

import com.ctg.constants.PaginationConstants;
import com.ctg.dto.PagedResponse;
import com.ctg.dto.UserDto;
import com.ctg.service.UserService;
import jakarta.validation.Valid;
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
            @RequestParam(value = "page",
                    defaultValue = PaginationConstants.DEFAULT_PAGE_NUMBER, required = false) int page,
            @RequestParam(value = "size",
                    defaultValue = PaginationConstants.DEFAULT_PAGE_SIZE, required = false) int size,
            @RequestParam(value = "sort",
                    defaultValue = PaginationConstants.DEFAULT_SORT_BY, required = false) String sort,
            @RequestParam(value = "sortDir",
                    defaultValue = PaginationConstants.DEFAULT_SORT_DIR, required = false) String sortDir
    ) {
       return userService.getPagedUsers(page, size, sort, sortDir);
    }

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable("id") Long id) {
        return userService.getUser(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createNewUser(@Valid @RequestBody UserDto newUser) {
        return userService.createUser(newUser);
    }

    @PutMapping("/{id}")
    public UserDto updateUser(@Valid @RequestBody UserDto updatedUser, @PathVariable("id") Long id) {
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
