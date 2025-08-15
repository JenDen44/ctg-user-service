package com.ctg.controller;

import com.ctg.common.BaseIntegrationTest;
import com.ctg.common.TestUserFactory;
import com.ctg.dto.PagedResponse;
import com.ctg.dto.UserDto;
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

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("ALTER TABLE users DISABLE TRIGGER ALL;");
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
        jdbcTemplate.execute("ALTER TABLE users ENABLE TRIGGER ALL;");
    }

    @Test
    @DisplayName("POST /users — OK")
    void createUserOK() {
        UserDto newUser = TestUserFactory.createUserDto();

        ResponseEntity<UserDto> response = restTemplate.postForEntity(
                MAIN_PATH,
                newUser,
                UserDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();

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
    @DisplayName("POST /users — invalid email formats")
    void createUserWithInvalidEmails() {
        String invalidEmail = "invalidEmail";
        UserDto userWithInvalidEmail = TestUserFactory.userDtoWithEmail(invalidEmail);

        ResponseEntity<String> stringResponse = restTemplate.postForEntity(
                MAIN_PATH,
                userWithInvalidEmail,
                String.class
        );

        assertThat(stringResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        String responseBody = stringResponse.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody)
                .contains(VALIDATION_TITLE)
                .contains(CODE_400)
                .contains("\"fields\":[{\"field\":\"email\",\"message\":\"Email should be valid\"}]");
    }

    @Test
    @DisplayName("POST /users — invalid passwords")
    void createUserWithInvalidPasswords() {
        String invalidPassword = "short";
        UserDto userWithInvalidPassword = TestUserFactory.userDtoWithPassword(invalidPassword);

        ResponseEntity<String> stringResponse = restTemplate.postForEntity(
                MAIN_PATH,
                userWithInvalidPassword,
                String.class
        );
        assertThat(stringResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        String responseBode = stringResponse.getBody();
        assertThat(responseBode).isNotNull();

        assertThat(responseBode)
                .contains(VALIDATION_TITLE)
                .contains(CODE_400)
                .contains("\"fields\":[{\"field\":\"password\",\"message\":\"Password must be 8-100 characters\"}]");
    }

    @Test
    @DisplayName("GET /users/{id} — OK")
    void getUserOK() {
        UserDto userFromDb = TestUserFactory.createUserDto();
        Long userId = insertUserAndGetId(userFromDb);

        ResponseEntity<UserDto> response = restTemplate.getForEntity(
                MAIN_PATH_ID + userId,
                UserDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .extracting(UserDto::getFullName, UserDto::getEmail)
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
        UserDto updatedUser = new UserDto(userId, "New Name", "new@mail.com", "newPassword", Role.ADMIN);

        HttpEntity<UserDto> request = new HttpEntity<>(updatedUser);

        ResponseEntity<UserDto> response = restTemplate.exchange(
                MAIN_PATH_ID + userId,
                HttpMethod.PUT,
                request,
                UserDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserDto responseUserDto = response.getBody();
        assertThat(responseUserDto.getFullName()).isEqualTo(updatedUser.getFullName());
        assertThat(responseUserDto.getRole()).isEqualTo(updatedUser.getRole());
        assertThat(responseUserDto.getId()).isEqualTo(updatedUser.getId());
        assertThat(responseUserDto.getEmail()).isEqualTo(updatedUser.getEmail());
    }

    @Test
    @DisplayName("PUT /users/{id} — invalid passwords")
    void updateUserWithInvalidPassword() {
        Long userId = createTestUser("Old Name", "old@mail.com", Role.EMPLOYEE);
        UserDto updatedUser = new UserDto(userId, "New Name", "new@mail.com", "short", Role.ADMIN);
        HttpEntity<UserDto> request = new HttpEntity<>(updatedUser);

        ResponseEntity<String> stringResponse = restTemplate.exchange(
                MAIN_PATH_ID + userId,
                HttpMethod.PUT,
                request,
                String.class
        );

        assertThat(stringResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String responseBody = stringResponse.getBody();
        assertThat(responseBody).isNotNull();

        assertThat(responseBody)
                .contains(VALIDATION_TITLE)
                .contains(CODE_400)
                .contains("\"fields\":[{\"field\":\"password\",\"message\":\"Password must be 8-100 characters\"}]");

        String oldName = jdbcTemplate.queryForObject(
                "SELECT full_name FROM users where id = " + userId,
                String.class
        );

        assertThat(oldName).isNotEqualTo(updatedUser);
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
    @DisplayName("PUT /users — invalid email formats")
    void updateUserWithInvalidEmails() {
        String invalidEmail = "invalidEmail";
        UserDto userWithInvalidEmail = TestUserFactory.userDtoWithEmail(invalidEmail);

        HttpEntity<UserDto> httpEntity = new HttpEntity<>(userWithInvalidEmail);

        ResponseEntity<String> stringResponse = restTemplate.exchange(
                MAIN_PATH_ID + 3,
                HttpMethod.PUT,
                httpEntity,
                String.class
        );

        assertThat(stringResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        String responseBody = stringResponse.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody)
                .contains(VALIDATION_TITLE)
                .contains(CODE_400)
                .contains("\"fields\":[{\"field\":\"email\",\"message\":\"Email should be valid\"}]");
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

        ResponseEntity<PagedResponse<UserDto>> response = restTemplate.exchange(
                MAIN_PATH + "?page=0&size=2",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PagedResponse<UserDto> pagedResponse = response.getBody();
        assertThat(pagedResponse.getContent())
                .hasSize(2)
                .extracting(UserDto::getEmail)
                .containsExactly("user1@mail.com", "user2@mail.com");
        assertThat(pagedResponse.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("GET /users — with default params")
    void getPagedUsersWithDefaultParams() {
        createThreeTestUsers();

        ResponseEntity<PagedResponse<UserDto>> response = restTemplate.exchange(
                MAIN_PATH,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PagedResponse<UserDto> pagedResponse = response.getBody();
        assertThat(pagedResponse.getContent())
                .hasSize(3)
                .extracting(UserDto::getEmail)
                .containsExactly("user1@mail.com", "user2@mail.com", "user3@mail.com");
        assertThat(pagedResponse.getTotalElements()).isEqualTo(3);
    }

    private Long insertUserAndGetId(UserDto user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (email, full_name, password, role) VALUES (?, ?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getRole().name());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    private Long createTestUser(String name, String email, Role role) {
        return insertUserAndGetId(
                UserDto.builder()
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
