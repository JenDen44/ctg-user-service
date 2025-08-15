package com.ctg.service;

import com.ctg.common.TestUserFactory;
import com.ctg.dto.PagedResponse;
import com.ctg.dto.UserDto;
import com.ctg.exceptions.ResourceNotFoundException;
import com.ctg.exceptions.ValidationException;
import com.ctg.mapper.UserMapper;
import com.ctg.model.ErrorField;
import com.ctg.model.User;
import com.ctg.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private final Long userId = TestUserFactory.USER_ID;
    private UserDto dto;
    private User entity;

    @BeforeEach
    void setup() {
        dto = TestUserFactory.createUserDto();
        entity = TestUserFactory.createUser();
    }


    @Test
    @DisplayName("Get user — found")
    void getUserFound() {
        when(userRepo.findById(userId)).thenReturn(Optional.of(entity));
        when(userMapper.toDto(entity)).thenReturn(dto);

        UserDto result = userService.getUser(userId);
        assertThat(result).isEqualTo(dto);
    }

    @Test
    @DisplayName("Get user — not found")
    void getUserNotFound() {
        when(userRepo.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @DisplayName("Get paged - with users")
    @Test
    void getPagedUsersWithUsers() {
        int page = 0, size = 10;
        String sortBy = "id", sortDir = "ASC";
        Page<User> userPage = new PageImpl<>(List.of(entity));

        when(userRepo.findAll(any(PageRequest.class))).thenReturn(userPage);
        when(userMapper.toDto(entity)).thenReturn(dto);

        PagedResponse<UserDto> pagedResponse = userService.getPagedUsers(page, size, sortBy, sortDir);

        assertThat(pagedResponse.getPageSize()).isEqualTo(1);
        assertThat(pagedResponse.getPageNumber()).isEqualTo(page);
        assertThat(pagedResponse.getContent()).hasSize(1);
    }

    @DisplayName("Get paged - empty content")
    @Test
    void getPagedUsersWhenUsersFound() {
        int page = 0, size = 10;
        String sortBy = "email", sortDir = "DESC";
        Page<User> userPage = new PageImpl<>(Collections.emptyList());

        when(userRepo.findAll(any(PageRequest.class))).thenReturn(userPage);

        PagedResponse<UserDto> pagedResponse = userService.getPagedUsers(page, size, sortBy, sortDir);

        assertThat(pagedResponse.getPageSize()).isEqualTo(0);
        assertThat(pagedResponse.getPageNumber()).isEqualTo(page);
        assertThat(pagedResponse.getContent()).hasSize(0);

        verify(userMapper, never()).toDto(entity);
    }

    @Test
    @DisplayName("Create user — created")
    void createUserCreated() {
        when(userRepo.existsByEmail(dto.getEmail())).thenReturn(false);
        when(userMapper.toEntity(dto)).thenReturn(entity);
        when(userRepo.save(entity)).thenReturn(entity);
        when(userMapper.toDto(entity)).thenReturn(dto);

        UserDto result = userService.createUser(dto);
        assertThat(result).isEqualTo(dto);
    }

    @Test
    @DisplayName("Create user — email already exists")
    void createUserEmailAlreadyExists() {
        when(userRepo.existsByEmail(dto.getEmail())).thenReturn(true);

        ValidationException ex = catchThrowableOfType(
                () -> userService.createUser(dto),
                ValidationException.class
        );

        assertThat(ex.getErrorFields())
                .extracting(ErrorField::getField)
                .containsExactly("email");

        verify(userRepo, never()).save(any());
    }

    @DisplayName("Update user - updated")
    @Test
    void updateUserUpdated() {
        UserDto updatedEmail = TestUserFactory.userDtoWithEmail("updatedEmail@yandex.com");
        when(userRepo.findById(userId)).thenReturn(Optional.of(entity));
        when(userRepo.existsByEmail(updatedEmail.getEmail())).thenReturn(false);
        when(userRepo.save(entity)).thenReturn(entity);
        when(userMapper.toDto(entity)).thenReturn(updatedEmail);

        UserDto updatedUser = userService.updateUser(updatedEmail, userId);

        assertThat(updatedUser).isEqualTo(updatedEmail);
    }

    @DisplayName("Update user - email already exists")
    @Test
    void updateUserEmailAlreadyExists() {
        entity.setId(userId);
        dto.setEmail("changedEmail@yandex.ru");
        when(userRepo.findById(userId)).thenReturn(Optional.of(entity));
        when(userRepo.existsByEmail(dto.getEmail())).thenReturn(true);

        ValidationException ex = catchThrowableOfType(
                () -> userService.updateUser(dto, userId),
                ValidationException.class
        );

        assertThat(ex.getErrorFields())
                .extracting(ErrorField::getField)
                .containsExactly("email");

        verify(userRepo, never()).save(any());
        verify(userMapper, never()).toDto(entity);
    }

    @DisplayName("Delete user - deleted")
    @Test
    void deleteUserDeleted() {
        when(userRepo.existsById(userId)).thenReturn(true);

        userService.deleteUser(userId);

        verify(userRepo).deleteById(userId);
    }

    @DisplayName("Delete user - not found")
    @Test
    void deleteUserNotFound() {
        when(userRepo.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() ->
                userService.deleteUser(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepo, never()).deleteById(userId);
    }
}