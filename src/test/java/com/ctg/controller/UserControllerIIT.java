package com.ctg.controller;

import com.ctg.common.BaseIntegrationTest;
import com.ctg.common.TestUserFactory;
import com.ctg.dto.PagedResponse;
import com.ctg.dto.UserRequest;
import com.ctg.dto.UserResponse;
import com.ctg.model.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import java.sql.PreparedStatement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserControllerIIT extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String MAIN_PATH = "/api/v1/users";
    private final String MAIN_PATH_ID = "/api/v1/users/";
    private static final String VALIDATION_TITLE = "\"title\":\"Validation failed\"";
    private static final String NOT_FOUND_TITLE = "\"title\":\"Not found\"";
    private static final String CODE_404 = "\"code\":404";
    private static final String CODE_400 = "\"code\":400";

    private static final String CODE_422 = "\"code\":422";

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("ALTER TABLE users DISABLE TRIGGER ALL;");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("ALTER TABLE users ENABLE TRIGGER ALL;");
    }

    @Test
    @DisplayName("POST /users — OK")
    void createUserOK() {
        UserRequest newUser = TestUserFactory.createUserRequest();

        ResponseEntity<UserResponse> response = restTemplate.postForEntity(
                MAIN_PATH,
                newUser,
                UserResponse.class
        );

        UserResponse result = response.getBody();

        assertAll("created user",
                () -> assertNotNull(result),
                () -> assertThat(result.getId()).isNotNull(),
                () -> assertThat(result.getFullName()).isNotNull(),
                () -> assertThat(result.getRole()).isNotNull(),
                () -> assertThat(result.getEmail()).isNotNull(),
                () -> assertEquals(response.getStatusCode(), HttpStatus.CREATED)
        );

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                newUser.getEmail()
        );

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /users — invalid JSON (missing fields)")
    void createUserInvalidJson() {
        String json = """
        {
            "fullName": "Test User",
            "email": "test@mail.com",
            "password": "password123"
        """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(json, headers);

        ResponseEntity<String> stringResponse = restTemplate.postForEntity(
                MAIN_PATH,
                request,
                String.class
        );

        assertThat(stringResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        String responseBody = stringResponse.getBody();
        assertThat(responseBody).isNotNull();

        assertThat(responseBody)
                .contains("timestamp")
                .contains("\"status\":400")
                .contains("\"error\":\"Bad Request\"");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users",
                Integer.class
        );

        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("POST /users — invalid parameters")
    void createUserWithInvalidInvalidParams() {
        UserRequest userWithInvalidParams = TestUserFactory
                .createUserRequestWithParams("short", "invalidEmail@", "i");

        ResponseEntity<String> stringResponse = restTemplate.postForEntity(
                MAIN_PATH,
                userWithInvalidParams,
                String.class
        );
        assertThat(stringResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        String responseBode = stringResponse.getBody();

        assertThat(responseBode).isNotNull();
        assertThat(responseBode)
                .contains(VALIDATION_TITLE)
                .contains(CODE_422)
                .contains("\"field\":\"fullName\",\"message\":\"Full name must be 2-100 characters\"")
                .contains("\"field\":\"password\",\"message\":\"Password must be 8-100 characters\"")
                .contains("\"field\":\"email\",\"message\":\"Email should be valid\"");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users",
                Integer.class
        );

        assertEquals(0, (int) count);
    }

    @Test
    @DisplayName("GET /users/{id} — OK")
    void getUserOK() {
        UserRequest userFromDb = TestUserFactory.createUserRequest();
        Long userId = insertUserAndGetId(userFromDb);

        ResponseEntity<UserResponse> response = restTemplate.getForEntity(
                MAIN_PATH_ID + userId,
                UserResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .extracting(UserResponse::getFullName, UserResponse::getEmail)
                .containsExactly(userFromDb.getFullName(), userFromDb.getEmail());
    }

    @Test
    @DisplayName("GET /users/{id} — Not Found")
    void getUserNotFound() {
        Long userId = 111L;

        ResponseEntity<String> stringResponse = restTemplate.getForEntity(
                MAIN_PATH_ID + userId,
                String.class
        );

        assertThat(stringResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        String responseBody = stringResponse.getBody();

        assertThat(responseBody).isNotNull();
        assertThat(responseBody)
                .contains(NOT_FOUND_TITLE)
                .contains("\"message\":\"User not found with id 111\"")
                .contains(CODE_404);
    }

    @Test
    @DisplayName("PUT /users/{id} — OK")
    void updateUserOK() {
        Long userId = createTestUser("Old Name", "old@mail.com", Role.EMPLOYEE);
        UserRequest updatedUser = new UserRequest("New Name", "new@mail.com", "newPassword", Role.ADMIN);

        HttpEntity<UserRequest> request = new HttpEntity<>(updatedUser);

        ResponseEntity<UserResponse> response = restTemplate.exchange(
                MAIN_PATH_ID + userId,
                HttpMethod.PUT,
                request,
                UserResponse.class
        );

        UserResponse result = response.getBody();

        assertAll("updated user",
                () -> assertNotNull(response),
                () -> assertEquals(response.getStatusCode(), HttpStatus.OK),
                () -> assertNotNull(result),
                () -> assertEquals(result.getFullName(), updatedUser.getFullName()),
                () -> assertEquals(result.getRole(), updatedUser.getRole()),
                () -> assertEquals(result.getEmail(), updatedUser.getEmail())
        );
    }

    @Test
    @DisplayName("PUT /users/{id} — invalid parameters")
    void updateUserWithInvalidParameters() {
        Long userId = createTestUser("Old Name", "old@mail.com", Role.EMPLOYEE);
        UserRequest userWithInvalidParams = TestUserFactory
                .createUserRequestWithParams("short", "invalidEmail@", "i");

        HttpEntity<UserRequest> request = new HttpEntity<>(userWithInvalidParams);

        ResponseEntity<String> stringResponse = restTemplate.exchange(
                MAIN_PATH_ID + userId,
                HttpMethod.PUT,
                request,
                String.class
        );

        assertThat(stringResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        String responseBode = stringResponse.getBody();

        assertThat(responseBode).isNotNull();
        assertThat(responseBode)
                .contains(VALIDATION_TITLE)
                .contains(CODE_422)
                .contains("\"field\":\"fullName\",\"message\":\"Full name must be 2-100 characters\"")
                .contains("\"field\":\"password\",\"message\":\"Password must be 8-100 characters\"")
                .contains("\"field\":\"email\",\"message\":\"Email should be valid\"");

        String oldName = jdbcTemplate.queryForObject(
                "SELECT full_name FROM users where id = " + userId,
                String.class
        );

        assertNotEquals(oldName, userWithInvalidParams.getFullName());
    }

    @Test
    @DisplayName("PUT /users — invalid JSON (missing fields)")
    void updateUserInvalidJson() {
        String json = """
        {
            "fullName": "Test User",
            "email": "test@mail.com",
            "password": "password123"
        """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(json, headers);

        ResponseEntity<String> stringResponse = restTemplate.exchange(
                MAIN_PATH_ID + 3,
                HttpMethod.PUT,
                request,
                String.class
        );

        assertThat(stringResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        String responseBody = stringResponse.getBody();

        assertThat(responseBody).isNotNull();
        assertThat(responseBody)
                .contains("timestamp")
                .contains("\"status\":400")
                .contains("\"error\":\"Bad Request\"");
    }

    @Test
    @DisplayName("DELETE /users/{id} — OK")
    void deleteUserOK() {
        Long userId = createTestUser("Tes User", "test@mail.com", Role.EMPLOYEE);

        restTemplate.delete(MAIN_PATH_ID + userId);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = " + userId,
                Integer.class
        );

        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("DELETE /users/{id} — Not Found")
    void deleteUserNotFound() {
        ResponseEntity<String> stringResponse = restTemplate.exchange(
                MAIN_PATH_ID + 999,
                HttpMethod.DELETE,
                null,
                String.class
        );

        assertThat(stringResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        String responseBody = stringResponse.getBody();

        assertThat(responseBody).isNotNull();
        assertThat(responseBody)
                .contains(NOT_FOUND_TITLE)
                .contains(CODE_404)
                .contains("\"message\":\"User not found with id: 999\"");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = " + 999,
                Integer.class
        );

        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("GET /users — With params")
    void getPagedUsersWithParams() {
       createThreeTestUsers();

        ResponseEntity<PagedResponse<UserResponse>> response = restTemplate.exchange(
                MAIN_PATH + "?page=0&size=2",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        PagedResponse<UserResponse> pagedResponse = response.getBody();

        assertThat(pagedResponse.getContent())
                .hasSize(2)
                .extracting(UserResponse::getEmail)
                .containsExactly("user1@mail.com", "user2@mail.com");
        assertThat(pagedResponse.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("GET /users — with default params")
    void getPagedUsersWithDefaultParams() {
        createThreeTestUsers();

        ResponseEntity<PagedResponse<UserResponse>> response = restTemplate.exchange(
                MAIN_PATH,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        PagedResponse<UserResponse> pagedResponse = response.getBody();

        assertThat(pagedResponse.getContent())
                .hasSize(3)
                .extracting(UserResponse::getEmail)
                .containsExactly("user1@mail.com", "user2@mail.com", "user3@mail.com");
        assertThat(pagedResponse.getTotalElements()).isEqualTo(3);
    }

    private Long insertUserAndGetId(UserRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (email, full_name, password, role) VALUES (?, ?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setString(1, request.getEmail());
            ps.setString(2, request.getFullName());
            ps.setString(3, request.getPassword());
            ps.setString(4, request.getRole().name());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    private Long createTestUser(String name, String email, Role role) {
        return insertUserAndGetId(
                UserRequest.builder()
                        .fullName(name)
                        .email(email)
                        .password("password")
                        .role(role)
                        .build()
        );
    }

    private void createThreeTestUsers() {
        createTestUser("User1", "user1@mail.com", Role.ADMIN);
        createTestUser("User2", "user2@mail.com", Role.ADMIN);
        createTestUser("User3", "user3@mail.com", Role.EMPLOYEE);
    }
}
