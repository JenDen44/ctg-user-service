package com.ctg.service;

import com.ctg.common.TestUserFactory;
import com.ctg.dto.PagedResponse;
import com.ctg.dto.UserRequest;
import com.ctg.dto.UserResponse;
import com.ctg.exceptions.ResourceNotFoundException;
import com.ctg.exceptions.ValidationException;
import com.ctg.mapper.UserMapper;
import com.ctg.dto.ErrorField;
import com.ctg.model.User;
import com.ctg.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private UserServiceImpl userService;

    private final Long userId = TestUserFactory.USER_ID;
    private UserRequest request;
    private UserResponse response;
    private User entity;

    @BeforeEach
    void setup() {
        request = TestUserFactory.createUserRequest();
        entity = TestUserFactory.createUser(userId);
        response = TestUserFactory.createUserResponse(userId);
    }


    @Test
    @DisplayName("Get user — found")
    void getUserFound() {
        when(userRepo.findById(userId)).thenReturn(Optional.of(entity));
        when(userMapper.toDto(entity)).thenReturn(response);

        UserResponse result = userService.get(userId);

        assertAll("found user",
                () -> assertNotNull(result),
                () -> assertEquals(result.getId(), response.getId()),
                () -> assertEquals(result.getFullName(), response.getFullName()),
                () -> assertEquals(result.getRole(), response.getRole()),
                () -> assertEquals(result.getEmail(), response.getEmail())
        );

        verify(userRepo, times(1)).findById(userId);
        verify(userMapper, times(1)).toDto(entity);
    }

    @Test
    @DisplayName("Get user — not found")
    void getUserNotFound() {
        when(userRepo.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.get(userId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepo, times(1)).findById(userId);
    }

    @DisplayName("Get paged - with users")
    @Test
    void getPagedUsersWithUsers() {
        int page = 0, size = 10;
        String sortBy = "id", sortDir = "ASC";
        Page<User> userPage = new PageImpl<>(List.of(entity));

        when(userRepo.findAll(any(PageRequest.class))).thenReturn(userPage);
        when(userMapper.toDto(entity)).thenReturn(response);

        PagedResponse<UserResponse> pagedResponse = userService.getByPage(page, size, sortBy, sortDir);

        List<UserResponse> pageContent = pagedResponse.getContent();
        UserResponse first = pageContent.getFirst();

        assertAll("found paged user",
                () -> assertNotNull(pagedResponse),
                () -> assertNotNull(first),
                () -> assertEquals(pagedResponse.getPageSize(), 1),
                () -> assertEquals(pagedResponse.getPageNumber(), page),
                () -> assertEquals(pageContent.size(), 1),
                () -> assertEquals(first.getId(), response.getId()),
                () -> assertEquals(first.getFullName(), response.getFullName()),
                () -> assertEquals(first.getRole(), response.getRole()),
                () -> assertEquals(first.getEmail(), response.getEmail())
        );

        verify(userRepo, times(1)).findAll(any(PageRequest.class));
        verify(userMapper, times(1)).toDto(entity);
    }

    @DisplayName("Get paged - empty content")
    @Test
    void getPagedUsersWhenUsersFound() {
        int page = 0, size = 10;
        String sortBy = "email", sortDir = "DESC";
        Page<User> userPage = new PageImpl<>(Collections.emptyList());

        when(userRepo.findAll(any(PageRequest.class))).thenReturn(userPage);

        PagedResponse<UserResponse> pagedResponse = userService.getByPage(page, size, sortBy, sortDir);

        assertAll("found paged user",
                () -> assertNotNull(pagedResponse),
                () -> assertEquals(pagedResponse.getPageSize(), 0),
                () -> assertEquals(pagedResponse.getPageNumber(), page),
                () -> assertEquals(pagedResponse.getContent().size(), 0));

        verify(userRepo, times(1)).findAll(any(PageRequest.class));
        verify(userMapper, never()).toDto(entity);
    }


    @Test
    @DisplayName("Create user — created")
    void createUserCreated() {
        when(userRepo.existsByEmail(request.getEmail())).thenReturn(false);
        when(encoder.encode(request.getPassword())).thenReturn("hashedPwd");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepo.save(any(User.class)))
                .thenAnswer(inv -> {
                    User u = inv.getArgument(0);
                    u.setId(10L);
                    return u;
                });
        when(userMapper.toDto(any(User.class))).thenReturn(response);

        var result = userService.create(request);

        assertAll("created user",
                () -> assertNotNull(result),
                () -> assertEquals(result.getId(), response.getId()),
                () -> assertEquals(result.getFullName(), response.getFullName()),
                () -> assertEquals(result.getRole(), response.getRole()),
                () -> assertEquals(result.getEmail(), response.getEmail())
        );

        verify(userRepo, times(1)).existsByEmail(request.getEmail());
        verify(userRepo, times(1)).save(userCaptor.capture());
        verify(userMapper, times(1)).toDto(userCaptor.capture());
        verify(encoder, times(1)).encode(request.getPassword());
    }

    @Test
    @DisplayName("Create user — email already exists")
    void createUserEmailAlreadyExists() {
        when(userRepo.existsByEmail(response.getEmail())).thenReturn(true);

        ValidationException ex = catchThrowableOfType(
                () -> userService.create(request),
                ValidationException.class
        );

        assertThat(ex.getErrorFields())
                .extracting(ErrorField::getField)
                .containsExactly("email");

        verify(userRepo, times(1)).existsByEmail(request.getEmail());
        verify(userMapper, never()).toEntity(request);
        verify(userRepo, never()).save(any());
    }

    @DisplayName("Update user - updated")
    @Test
    void updateUserUpdated() {
        UserRequest updatedEmail = TestUserFactory.createUserRequestWithEmail("updatedEmail@yandex.com");
        UserResponse updatedResponse = TestUserFactory.createUserResponse(userId, updatedEmail);
        when(userRepo.findById(userId)).thenReturn(Optional.of(entity));
        when(userRepo.existsByEmail(updatedEmail.getEmail())).thenReturn(false);
        when(userMapper.toDto(entity)).thenReturn(updatedResponse);

        UserResponse result = userService.update(userId, updatedEmail);

        assertAll("updated user",
                () -> assertNotNull(result),
                () -> assertEquals(result.getId(), updatedResponse.getId()),
                () -> assertEquals(result.getFullName(), updatedResponse.getFullName()),
                () -> assertEquals(result.getRole(), updatedResponse.getRole()),
                () -> assertEquals(result.getEmail(), updatedResponse.getEmail())
        );

        verify(userRepo, times(1)).findById(userId);
        verify(userRepo, times(1)).existsByEmail(updatedEmail.getEmail());
        verify(userMapper, times(1)).toDto(entity);
    }

    @DisplayName("Update user - email already exists")
    @Test
    void updateUserEmailAlreadyExists() {
        UserRequest updatedUser = TestUserFactory.createUserRequestWithEmail("changedEmail@yandex.ru");
        when(userRepo.findById(userId)).thenReturn(Optional.of(entity));
        when(userRepo.existsByEmail(updatedUser.getEmail())).thenReturn(true);

        ValidationException ex = catchThrowableOfType(
                () -> userService.update(userId, updatedUser),
                ValidationException.class
        );

        assertThat(ex.getErrorFields())
                .extracting(ErrorField::getField)
                .containsExactly("email");

        verify(userRepo, times(1)).findById(userId);
        verify(userRepo, times(1)).existsByEmail(updatedUser.getEmail());
        verify(userMapper, never()).toDto(entity);
    }

    @DisplayName("Delete user - deleted")
    @Test
    void deleteUserDeleted() {
        when(userRepo.existsById(userId)).thenReturn(true);

        userService.delete(userId);

        verify(userRepo, times(1)).existsById(userId);
        verify(userRepo).deleteById(userId);
    }

    @DisplayName("Delete user - not found")
    @Test
    void deleteUserNotFound() {
        when(userRepo.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() ->
                userService.delete(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepo, times(1)).existsById(userId);
        verify(userRepo, never()).deleteById(userId);
    }

    @DisplayName("findByEmailForLogin - found")
    void findByEmailFound() {
        when(userRepo.findByEmail(entity.getEmail())).thenReturn(Optional.of(entity));

       User foundUser = userService.findByEmail(entity.getEmail());

        assertThat(foundUser).isEqualTo(entity);
    }

    @Test
    @DisplayName("findByEmailForLogin - not found")
    void findByEmailNotFound() {
        String notExistingEmail = "notExistingEmail@mail.com";
        when(userRepo.findByEmail(notExistingEmail)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmail(notExistingEmail))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("increment tokenVersion - incremented")
    void incrementTokenVersionIncremented() {
        when(userRepo.findById(userId)).thenReturn(Optional.of(entity));

        userService.incrementTokenVersion(userId);

        assertThat(entity.getTokenVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("increment TokenVersion - not found")
    void incrementTokenVersionNotFound() {
        Long notExistingId = 999L;
        when(userRepo.findById(notExistingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.incrementTokenVersion(notExistingId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
