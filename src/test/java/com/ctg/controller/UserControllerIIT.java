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
                .contains("\"title\":\"Validation failed\"")
                .contains("\"code\":400")
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
                .contains("\"title\":\"Validation failed\"")
                .contains("\"code\":400")
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
                .contains("\"title\":\"Not found\"")
                .contains("\"message\":\"User not found with id 111\"")
                .contains("\"code\":404");
    }

    @Test
    @DisplayName("PUT /users/{id} — OK")
    void updateUserOK() {
        jdbcTemplate.execute(
                "INSERT INTO users (id, full_name, email, password, role) " +
                        "VALUES (3, 'Old Name', 'old@mail.com', 'oldPassword', 'EMPLOYEE')"
        );

        UserDto updatedUser = new UserDto(3L, "New Name", "new@mail.com", "newPassword", Role.ADMIN);
        HttpEntity<UserDto> request = new HttpEntity<>(updatedUser);

        ResponseEntity<UserDto> response = restTemplate.exchange(
                MAIN_PATH_ID + 3,
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

        String email = jdbcTemplate.queryForObject(
                "SELECT email FROM users WHERE id = 3",
                String.class
        );
        assertThat(email).isEqualTo(responseUserDto.getEmail());
    }

    @Test
    @DisplayName("PUT /users/{id} — invalid passwords")
    void updateUserWithInvalidPassword() {
        jdbcTemplate.execute(
                "INSERT INTO users (id, full_name, email, password, role) " +
                        "VALUES (3, 'Old Name', 'old@mail.com', 'oldPassword', 'EMPLOYEE')"
        );

        UserDto updatedUser = TestUserFactory.userDtoWithIdAndPassword(3L, "short");
        HttpEntity<UserDto> request = new HttpEntity<>(updatedUser);

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
                .contains("\"title\":\"Validation failed\"")
                .contains("\"code\":400")
                .contains("\"fields\":[{\"field\":\"password\",\"message\":\"Password must be 8-100 characters\"}]");

        String oldName = jdbcTemplate.queryForObject(
                "SELECT full_name FROM users where id = 3",
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
                .contains("\"title\":\"Validation failed\"")
                .contains("\"code\":400")
                .contains("\"fields\":[{\"field\":\"email\",\"message\":\"Email should be valid\"}]");
    }

    @Test
    @DisplayName("DELETE /users/{id} — OK")
    void deleteUserOK() {
        jdbcTemplate.execute(
                "INSERT INTO users (id, full_name, email, password, role) " +
                        "VALUES (5, 'To Delete', 'delete@mail.com', 'pass', 'EMPLOYEE')"
        );

        restTemplate.delete(MAIN_PATH_ID + 5);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = 5",
                Integer.class
        );
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("DELETE /users/{id} — Not Found")
    void deleteUserNotFound() {
        ResponseEntity<String> stringResponse = restTemplate.exchange(
                MAIN_PATH_ID + 5,
                HttpMethod.DELETE,
                null,
                String.class
        );

        assertThat(stringResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        String responseBody = stringResponse.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody)
                .contains("\"title\":\"Not found\"")
                .contains("\"code\":404")
                .contains("\"message\":\"User not found with id: 5\"");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = 5",
                Integer.class
        );
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("GET /users — With params")
    void getPagedUsersWithParams() {
        jdbcTemplate.execute("INSERT INTO users (full_name, email, password, role) VALUES " +
                "('User1', 'user1@mail.com', 'pass1', 'ADMIN'), " +
                "('User2', 'user2@mail.com', 'pass2', 'ADMIN'), " +
                "('User3', 'user3@mail.com', 'pass3', 'EMPLOYEE')");

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
        jdbcTemplate.execute("INSERT INTO users (full_name, email, password, role) VALUES " +
                "('User1', 'user1@mail.com', 'pass1', 'ADMIN'), " +
                "('User2', 'user2@mail.com', 'pass2', 'ADMIN'), " +
                "('User3', 'user3@mail.com', 'pass3', 'EMPLOYEE')");

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
}
