package com.ctg.service;

import com.ctg.dto.*;
import com.ctg.exceptions.ResourceNotFoundException;
import com.ctg.exceptions.ValidationException;
import com.ctg.model.User;
import com.ctg.repository.UserRepository;
import com.ctg.mapper.UserMapper;
import com.ctg.security.CurrentUserIdExtractor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder encoder;
    private final CurrentUserIdExtractor currentUserIdExtractor;

    @Override
    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> {
                   log.error("User not found with id {}", id);
                   return new ResourceNotFoundException("User not found with id " + id);
                });
    }

    @Override
    public UserResponse create(@Valid UserRequest userDto) {
        if (userRepository.existsByEmail(userDto.getEmail())) {
            log.error("Email already exists {}", userDto.getEmail());
            throw new ValidationException(List.of(new ErrorField("email", "Email already exists")));
        }

        var newUser = User.builder()
                .email(userDto.getEmail())
                .role(userDto.getRole())
                .fullName(userDto.getFullName())
                .password(encoder.encode(userDto.getPassword()))
                .tokenVersion(0)
                .build();

        var savedUser = userRepository.save(newUser);
        log.debug("Created new user {}", newUser);

        return userMapper.toDto(savedUser);
    }

    @Override
    public UserResponse update(Long id, @Valid UserRequest userDto) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    if (!existingUser.getEmail().equals(userDto.getEmail())
                            && userRepository.existsByEmail(userDto.getEmail())) {
                        log.error("Email already exists {}", userDto.getEmail());
                        throw new ValidationException(List.of(new ErrorField("email", "Email already exists")));
                    }
                    userMapper.updateEntityFromDto(userDto, existingUser);
                    log.debug("Newly updated user {}", existingUser);

                    return userMapper.toDto(existingUser);

                }).orElseThrow(() -> {
                    log.error("User not found with id {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });
    }

    @Override
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            log.error("User not found with id {}", id);
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
        log.debug("User is deleted by id {}", id);
    }

    @Transactional(readOnly = true)
    @Override
    public PagedResponse<UserResponse> getByPage(int pageNo, int pageSize, String sortBy, String sortDir) {
        log.info("page params : {}, {}, {}, {}", pageNo, pageSize, sortBy, sortDir);

        var direction = Sort.Direction.fromOptionalString(sortDir).orElse(Sort.Direction.ASC);
        var sort = Sort.by(direction, sortBy);
        var pageRequest = PageRequest.of(pageNo, pageSize, sort);

        var pagedUsers = userRepository.findAll(pageRequest);
        log.debug("Paged users count {}, total pages {}, total elements {}",
                pagedUsers.getSize(), pagedUsers.getTotalPages(), pagedUsers.getTotalElements());

        var pageList = pagedUsers
                .stream()
                .map(userMapper::toDto)
                .toList();

        return new PagedResponse<>(
                pageList,
                pagedUsers.getNumber(),
                pagedUsers.getSize(),
                pagedUsers.getTotalElements(),
                pagedUsers.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public LoginUserResponse findByEmailForLogin(String email) {
        User foundUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return LoginUserResponse.builder()
                .id(foundUser.getId())
                .email(foundUser.getEmail())
                .passwordHash(foundUser.getPassword())
                .role(foundUser.getRole())
                .tokenVersion(foundUser.getTokenVersion())
                .build();
    }

    public void incrementTokenVersion(Long id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setTokenVersion(user.getTokenVersion() + 1);
    }

    @Override
    public UserResponse getByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Not found by email " + email));
    }

    @Override
    public UserResponse getCurrent(Jwt jwt) {
        CurrentUserIdExtractor.UserId userId = currentUserIdExtractor.resolve(jwt);

        return switch (userId) {
            case CurrentUserIdExtractor.UserId.Numeric id -> get(id.value());
            case CurrentUserIdExtractor.UserId.Subject email -> getByEmail(email.value());
        };
    }
}
