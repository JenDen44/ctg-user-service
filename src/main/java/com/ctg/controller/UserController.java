package com.ctg.controller;

import com.ctg.constants.PaginationConstants;
import com.ctg.dto.PagedResponse;
import com.ctg.dto.UserRequest;
import com.ctg.dto.UserResponse;
import com.ctg.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @GetMapping
    public PagedResponse<UserResponse> getUsers(
            @RequestParam(value = "page",
                    defaultValue = PaginationConstants.DEFAULT_PAGE_NUMBER, required = false) int page,
            @RequestParam(value = "size",
                    defaultValue = PaginationConstants.DEFAULT_PAGE_SIZE, required = false) int size,
            @RequestParam(value = "sort",
                    defaultValue = PaginationConstants.DEFAULT_SORT_BY, required = false) String sort,
            @RequestParam(value = "sortDir",
                    defaultValue = PaginationConstants.DEFAULT_SORT_DIR, required = false) String sortDir
    ) {
       return userService.getByPage(page, size, sort, sortDir);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable("id") Long id) {
        return userService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserRequest newUser) {
        return userService.create(newUser);
    }

    @PutMapping("/{id}")
    public UserResponse update(@Valid @RequestBody UserRequest updatedUser, @PathVariable("id") Long id) {
        return userService.update(id, updatedUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        userService.delete(id);
    }

    @GetMapping("/current")
    public UserResponse current(@AuthenticationPrincipal Jwt jwt) {
        return userService.getCurrent(jwt);
    }
}
