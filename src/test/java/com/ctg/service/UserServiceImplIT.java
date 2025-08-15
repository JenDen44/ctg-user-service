package com.ctg.service;

import com.ctg.common.BaseIntegrationTest;
import com.ctg.common.TestUserFactory;
import com.ctg.dto.PagedResponse;
import com.ctg.dto.UserDto;
import com.ctg.exceptions.ResourceNotFoundException;
import com.ctg.exceptions.ValidationException;
import com.ctg.model.Role;
import com.ctg.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class UserServiceImplIT extends BaseIntegrationTest {

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private UserRepository userRepository;

    @DisplayName("Create user when user created")
    @Test
    @Transactional
    void createUserWhenUserCreated() {
        UserDto newUser = TestUserFactory.createUserDto();

        UserDto savedUser = userService.createUser(newUser);
        UserDto retrievedUser = userService.getUser(savedUser.getId());

        assertThat(retrievedUser).isNotNull();
        assertThat(retrievedUser.getEmail()).isEqualTo(newUser.getEmail());
        assertThat(retrievedUser.getRole()).isEqualTo(newUser.getRole());
        assertThat(retrievedUser.getId()).isNotNull();
    }

    @DisplayName("Create user when email already exists")
    @Test
    void createUserWhenEmailAlreadyExists() {
        UserDto newUser = TestUserFactory.createUserDto();
        UserDto userWithDuplicateEmail = TestUserFactory.userDtoWithEmail(newUser.getEmail());

        userRepository.deleteAll();
        userService.createUser(newUser);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.createUser(userWithDuplicateEmail));

        assertThat(exception.getErrorFields())
                .extracting("field", "message")
                .containsExactly(tuple("email", "Email already exists"));

        assertEquals(1, userRepository.count());
    }

    @DisplayName("Update user when user updated")
    @Test
    void updateUserWhenUserUpdated() {
        UserDto userDto = TestUserFactory.createUserDto();
        UserDto user = userService.createUser(userDto);
        userDto.setEmail("updated@mail.com");
        userDto.setFullName("Updated fullName");

        UserDto updated = userService.updateUser(userDto, user.getId());

        assertThat(updated.getFullName()).isEqualTo(userDto.getFullName());
        assertThat(updated.getEmail()).isEqualTo(userDto.getEmail());
    }

    @DisplayName("Update user when user not found")
    @Test
    void updateUserWhenUserNotFound() {
        userRepository.deleteAll();
        UserDto userDto = TestUserFactory.createUserDto();

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUser(userDto, 7L));

        assertThat(exception.getMessage())
                .isEqualTo("User not found with id: 7");

        assertEquals(0, userRepository.count());
    }

    @DisplayName("Update user when email already exists")
    @Test
    void updateUserWhenEmailAlreadyExits() {
        userRepository.deleteAll();
        UserDto userDto1 = TestUserFactory.createUserDto();
        UserDto createdUser = userService.createUser(userDto1);
        UserDto userDto2 = TestUserFactory.userDtoWithEmail("duplicate@yandex.ru");
        userService.createUser(userDto2);
        userDto1.setEmail("duplicate@yandex.ru");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.updateUser(userDto1, createdUser.getId()));

        assertThat(exception.getErrorFields())
                .extracting("field", "message")
                .containsExactly(tuple("email", "Email already exists"));

        assertEquals(2, userRepository.count());
    }

    @DisplayName("Get user when user exists")
    @Test
    void getUserWhenUserExists() {
        UserDto user = TestUserFactory.createUserDto();
        UserDto existUser = userService.createUser(user);

        UserDto result = userService.getUser(existUser.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(existUser.getId());
        assertThat(result.getFullName()).isEqualTo(existUser.getFullName());
        assertThat(result.getEmail()).isEqualTo(existUser.getEmail());
        assertThat(result.getRole()).isEqualTo(existUser.getRole());
    }

    @DisplayName("Get user when user not found")
    @Test
    void getUserWhenUserNotFound() {
        Long nonExistentId = 999L;

        assertThatThrownBy(() -> userService.getUser(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id " + nonExistentId);
    }

    @DisplayName("Get paged users when sorted by fullName")
    @Test
    void getPagedUsersWhenSortedByFullName() {
        createTestUsers(5);

        PagedResponse<UserDto> result = userService.getPagedUsers(
                0,
                3,
                "fullName",
                "ASC"
        );

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getPageNumber()).isEqualTo(0);
        assertThat(result.getPageSize()).isEqualTo(3);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(2);

        List<String> names = result.getContent().stream()
                .map(UserDto::getFullName)
                .toList();
        assertThat(names).isSorted();
    }

    @DisplayName("Get paged users when no users found")
    @Test
    void getPagedUsersWhenNoUsersFound() {
        userRepository.deleteAll();

        PagedResponse<UserDto> result = userService.getPagedUsers(
                0,
                10,
                "id",
                "ASC"
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @DisplayName("Get paged users with different params")
    @Test
    void getPagedUsersWithDifferentParams() {
        createTestUsers(15);

        PagedResponse<UserDto> desc = userService.getPagedUsers(0, 5, "fullName", "DESC");
        assertThat(desc.getContent()).extracting(UserDto::getFullName).isSortedAccordingTo(Comparator.reverseOrder());

        PagedResponse<UserDto> page2 = userService.getPagedUsers(1, 5, "id", "ASC");
        assertThat(page2.getContent()).extracting(UserDto::getId).isSortedAccordingTo(Comparator.naturalOrder());
    }

    @DisplayName("Delete user when user deleted")
    @Test
    void deleteUserWhenUserDeleted() {
        UserDto user = TestUserFactory.createUserDto();
        UserDto userTobeDeleted = userService.createUser(user);

        userService.deleteUser(userTobeDeleted.getId());

        assertEquals(userRepository.existsById(userTobeDeleted.getId()), false);
    }

    @DisplayName("Delete user when user not found")
    @Test
    void deleteUserWhenUserNotFound() {
        userRepository.deleteAll();
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.deleteUser(1L));

        assertThat(exception.getMessage())
                .isEqualTo("User not found with id: 1");
    }

    private void createTestUsers(int count) {
        for (int i = 1; i <= count; i++) {
            UserDto user = new UserDto(
                    null,
                    "User " + i,
                    "user" + i + "@example.com",
                    "password" + i,
                    i % 2 == 0 ? Role.ADMIN : Role.EMPLOYEE
            );
            userService.createUser(user);
        }
    }
}
