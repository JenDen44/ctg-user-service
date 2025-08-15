package com.ctg.controller;

import com.ctg.common.TestUserFactory;
import com.ctg.dto.PagedResponse;
import com.ctg.dto.UserDto;
import com.ctg.exceptions.ResourceNotFoundException;
import com.ctg.exceptions.ValidationException;
import com.ctg.model.ErrorField;
import com.ctg.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private final Long userId = TestUserFactory.USER_ID;
    private static final String MAIN_PATH = "/api/v1/users";
    private static final String MAIN_PATH_ID = "/api/v1/users/{id}";

    private String toJson(Object obj) throws JsonProcessingException {
        return objectMapper.writeValueAsString(obj);
    }

    @Test
    @DisplayName("GET /users/{id} — OK")
    void getUserOK() throws Exception {
        UserDto userDto = TestUserFactory.createUserDto(userId);
        given(userService.getUser(userId)).willReturn(userDto);

        mockMvc.perform(get(MAIN_PATH_ID, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.fullName").value(userDto.getFullName()))
                .andExpect(jsonPath("$.email").value(userDto.getEmail()))
                .andExpect(jsonPath("$.role").value(userDto.getRole().name()));
    }

    @Test
    @DisplayName("GET /users/{id} — 404 Not Found")
    void getUserNotFound() throws Exception {
        String message = "User not found with id: " + userId;
        given(userService.getUser(userId)).willThrow(new ResourceNotFoundException(message));

        mockMvc.perform(get(MAIN_PATH_ID, userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    @DisplayName("GET /users with params — 200 OK")
    void getPagedUsersWithParams() throws Exception {
        var pagedResponse = TestUserFactory.pagedResponse();
        given(userService.getPagedUsers(anyInt(), anyInt(), anyString(), anyString()))
                .willReturn(pagedResponse);

        mockMvc.perform(get(MAIN_PATH)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id")
                        .param("sortDir", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(pagedResponse.getContent().getFirst().getId()))
                .andExpect(jsonPath("$.pageNumber").value(pagedResponse.getPageNumber()))
                .andExpect(jsonPath("$.pageSize").value(pagedResponse.getPageSize()))
                .andExpect(jsonPath("$.totalElements").value(pagedResponse.getTotalElements()))
                .andExpect(jsonPath("$.totalPages").value(pagedResponse.getTotalPages()));

        verify(userService).getPagedUsers(anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("GET /users with default params — 200 OK")
    void getPagedUsersWithDefaultParams() throws Exception {
        PagedResponse<UserDto> pagedResponse = TestUserFactory.pagedResponse();

        given(userService.getPagedUsers(anyInt(), anyInt(), anyString(), anyString()))
                .willReturn(pagedResponse);

        mockMvc.perform(get(MAIN_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()").value(pagedResponse.getContent().size()))
                .andExpect(jsonPath("$.pageNumber").value(pagedResponse.getPageNumber()))
                .andExpect(jsonPath("$.pageSize").value(pagedResponse.getPageSize()))
                .andExpect(jsonPath("$.totalElements").value(pagedResponse.getTotalElements()))
                .andExpect(jsonPath("$.totalPages").value(pagedResponse.getTotalPages()));

        verify(userService).getPagedUsers(anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /users — OK")
    void createUserOK() throws Exception {
        UserDto userDto = TestUserFactory.createUserDto(userId);

        given(userService.createUser(any(UserDto.class))).willReturn(userDto);

        mockMvc.perform(post(MAIN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(userDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.fullName").value(userDto.getFullName()))
                .andExpect(jsonPath("$.email").value(userDto.getEmail()))
                .andExpect(jsonPath("$.role").value(userDto.getRole().name()));
    }

    @Test
    @DisplayName("POST /users — invalid JSON (missing fields)")
    void createUserInvalidJson() throws Exception {
        String invalidJson = """
                {
                    "email": "user@mail.com"
                }
                """;

        mockMvc.perform(post(MAIN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalidEmail", "user@", "user@.", "user@com"})
    @DisplayName("POST /users — invalid email formats")
    void createUserWithInvalidEmails(String email) throws Exception {
        UserDto dto = TestUserFactory.userDtoWithEmail(email);
        doThrow(new ValidationException(List.of(new ErrorField("email", "Email should be valid"))))
                .when(userService).createUser(any());

        mockMvc.perform(post(MAIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("email"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "short", "pass12"})
    @DisplayName("POST /users — invalid passwords")
    void createUserWithInvalidPasswords(String password) throws Exception {
        UserDto dto = TestUserFactory.userDtoWithPassword(password);
        doThrow(new ValidationException(List.of(new ErrorField("password", "Password must be 8-100 characters"))))
                .when(userService).createUser(any());

        mockMvc.perform(post(MAIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("password"));
    }

    @DisplayName("PUT /users/{id} — OK")
    @Test
    void updateUserOk() throws Exception {
        UserDto userDto = TestUserFactory.createUserDto(userId);
        given(userService.updateUser(any(UserDto.class), eq(userId))).willReturn(userDto);

        mockMvc.perform(put(MAIN_PATH_ID, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.fullName").value(userDto.getFullName()))
                .andExpect(jsonPath("$.email").value(userDto.getEmail()))
                .andExpect(jsonPath("$.role").value(userDto.getRole().name()));

        verify(userService).updateUser(any(UserDto.class), anyLong());
    }

    @DisplayName("PUT /users/{id} — USER NOT FOUND")
    @Test
    void updateUserNotFound() throws Exception {
        String message = "User not found with id: " + userId;
        UserDto userDto = TestUserFactory.createUserDto(userId);
        doThrow(new ResourceNotFoundException(message))
                .when(userService).updateUser(any(UserDto.class), eq(userId));

        mockMvc.perform(put(MAIN_PATH_ID, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    @DisplayName("PUT /users/{id} — invalid JSON (broken structure)")
    void updateUserWithBrokenJson() throws Exception {
        String brokenJson = """
                { "email": "user@mail.com",
                  "password": "Password123"
                """;

        mockMvc.perform(put(MAIN_PATH_ID, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brokenJson))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalidEmail", "user@", "user@."})
    @DisplayName("POST /users — invalid email formats")
    void updateUserWithInvalidEmails(String email) throws Exception {
        UserDto userDto = TestUserFactory.userDtoWithEmail(email);
        doThrow(new ValidationException(List.of(new ErrorField("email", "Email should be valid"))))
                .when(userService).updateUser(userDto, userId);

        mockMvc.perform(put(MAIN_PATH_ID, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(userDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("email"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "short", "pass12"})
    @DisplayName("POST /users — invalid passwords")
    void updateUserWithInvalidPasswords(String password) throws Exception {
        UserDto userDto = TestUserFactory.userDtoWithPassword(password);
        doThrow(new ValidationException(List.of(new ErrorField("password", "Password must be 8-100 characters"))))
                .when(userService).updateUser(userDto, userId);

        mockMvc.perform(post(MAIN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(userDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("password"));
    }

    @Test
    @DisplayName("DELETE /users/{id} — OK")
    void deleteUserOK() throws Exception {
        mockMvc.perform(delete(MAIN_PATH_ID, userId))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(userId);
    }

    @Test
    @DisplayName("DELETE /users/{id} — NOT FOUND")
    void deleteUserNotFound() throws Exception {
        String message = "User not found with id: " + userId;

        doThrow(new ResourceNotFoundException(message))
                .when(userService).deleteUser(userId);

        mockMvc.perform(delete(MAIN_PATH_ID, userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(message));
    }
}