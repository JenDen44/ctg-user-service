package com.ctg.controller;

import com.ctg.common.TestUserFactory;
import com.ctg.dto.UserRequest;
import com.ctg.exceptions.ResourceNotFoundException;
import com.ctg.exceptions.ValidationException;
import com.ctg.dto.ErrorField;
import com.ctg.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
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
        var response = TestUserFactory.createUserResponse(userId);
        given(userService.get(userId)).willReturn(response);

        mockMvc.perform(get(MAIN_PATH_ID, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.fullName").value(response.getFullName()))
                .andExpect(jsonPath("$.email").value(response.getEmail()))
                .andExpect(jsonPath("$.role").value(response.getRole().name()));

        verify(userService, times(1)).get(userId);
    }

    @Test
    @DisplayName("GET /users/{id} — 404 Not Found")
    void getUserNotFound() throws Exception {
        String message = "User not found with id: " + userId;
        given(userService.get(userId)).willThrow(new ResourceNotFoundException(message));

        mockMvc.perform(get(MAIN_PATH_ID, userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(message));

        verify(userService, times(1)).get(userId);
    }

    @Test
    @DisplayName("GET /users with params — 200 OK")
    void getPagedUsersWithParams() throws Exception {
        var pagedResponse = TestUserFactory.pagedResponse();
        given(userService.getByPage(anyInt(), anyInt(), anyString(), anyString()))
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

        verify(userService).getByPage(anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("GET /users with default params — 200 OK")
    void getPagedUsersWithDefaultParams() throws Exception {
        var pagedResponse = TestUserFactory.pagedResponse();

        given(userService.getByPage(anyInt(), anyInt(), anyString(), anyString()))
                .willReturn(pagedResponse);

        mockMvc.perform(get(MAIN_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()").value(pagedResponse.getContent().size()))
                .andExpect(jsonPath("$.pageNumber").value(pagedResponse.getPageNumber()))
                .andExpect(jsonPath("$.pageSize").value(pagedResponse.getPageSize()))
                .andExpect(jsonPath("$.totalElements").value(pagedResponse.getTotalElements()))
                .andExpect(jsonPath("$.totalPages").value(pagedResponse.getTotalPages()));

        verify(userService).getByPage(anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /users — OK")
    void createUserOK() throws Exception {
        var request = TestUserFactory.createUserRequest();
        var response = TestUserFactory.createUserResponse(userId, request);

        given(userService.create(any(UserRequest.class))).willReturn(response);

        mockMvc.perform(post(MAIN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.fullName").value(response.getFullName()))
                .andExpect(jsonPath("$.email").value(response.getEmail()))
                .andExpect(jsonPath("$.role").value(response.getRole().name()));

        verify(userService, times(1)).create(any(UserRequest.class));
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
                .andExpect(status().isUnprocessableEntity());

        verify(userService, never()).create(any(UserRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalidEmail", "user@", "@.", "user"})
    @DisplayName("POST /users — invalid email formats")
    void createUserWithInvalidEmails(String email) throws Exception {
        var request = TestUserFactory.createUserRequestWithEmail(email);
        doThrow(new ValidationException(List.of(new ErrorField("email", "Email should be valid"))))
                .when(userService).create(any(UserRequest.class));

        mockMvc.perform(post(MAIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fields[0].field").value("email"));

        verify(userService, never()).create(any(UserRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "short", "pass12"})
    @DisplayName("POST /users — invalid passwords")
    void createUserWithInvalidPasswords(String password) throws Exception {
        var request = TestUserFactory.createUserRequestWithPassword(password);
        doThrow(new ValidationException(List.of(new ErrorField("password", "Password must be 8-100 characters"))))
                .when(userService).create(any());

        mockMvc.perform(post(MAIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fields[0].field").value("password"));

        verify(userService, never()).create(any(UserRequest.class));
    }

    @DisplayName("PUT /users/{id} — OK")
    @Test
    void updateUserOk() throws Exception {
        var request = TestUserFactory.createUserRequest();
        var response = TestUserFactory.createUserResponse(userId, request);
        given(userService.update(eq(userId), any(UserRequest.class))).willReturn(response);

        mockMvc.perform(put(MAIN_PATH_ID, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.fullName").value(response.getFullName()))
                .andExpect(jsonPath("$.email").value(response.getEmail()))
                .andExpect(jsonPath("$.role").value(response.getRole().name()));

        verify(userService).update(anyLong(), any(UserRequest.class));
    }

    @DisplayName("PUT /users/{id} — USER NOT FOUND")
    @Test
    void updateUserNotFound() throws Exception {
        String message = "User not found with id: " + userId;
        var request = TestUserFactory.createUserRequest();
        doThrow(new ResourceNotFoundException(message))
                .when(userService).update(eq(userId), any(UserRequest.class));

        mockMvc.perform(put(MAIN_PATH_ID, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(message));

        verify(userService).update(anyLong(), any(UserRequest.class));
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

        verify(userService, never()).update(anyLong(), any(UserRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalidEmail", "user@", "user@."})
    @DisplayName("POST /users — invalid email formats")
    void updateUserWithInvalidEmails(String email) throws Exception {
        var request = TestUserFactory.createUserRequestWithEmail(email);
        doThrow(new ValidationException(List.of(new ErrorField("email", "Email should be valid"))))
                .when(userService).update(userId, request);

        mockMvc.perform(put(MAIN_PATH_ID, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fields[0].field").value("email"));

        verify(userService, never()).update(anyLong(), any(UserRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "short", "pass12"})
    @DisplayName("POST /users — invalid passwords")
    void updateUserWithInvalidPasswords(String password) throws Exception {
        var request = TestUserFactory.createUserRequestWithPassword(password);

        doThrow(new ValidationException(List.of(new ErrorField("password", "Password must be 8-100 characters"))))
                .when(userService).update(userId, request);

        mockMvc.perform(post(MAIN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fields[0].field").value("password"));

        verify(userService, never()).update(anyLong(), any(UserRequest.class));
    }

    @Test
    @DisplayName("DELETE /users/{id} — OK")
    void deleteUserOK() throws Exception {
        mockMvc.perform(delete(MAIN_PATH_ID, userId))
                .andExpect(status().isNoContent());

        verify(userService).delete(userId);
    }

    @Test
    @DisplayName("DELETE /users/{id} — NOT FOUND")
    void deleteUserNotFound() throws Exception {
        String message = "User not found with id: " + userId;

        doThrow(new ResourceNotFoundException(message))
                .when(userService).delete(userId);

        mockMvc.perform(delete(MAIN_PATH_ID, userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(message));

        verify(userService).delete(userId);
    }
}
