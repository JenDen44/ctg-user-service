package com.ctg.service;

import com.ctg.dto.PagedResponse;
import com.ctg.exceptions.ResourceNotFoundException;
import com.ctg.exceptions.ValidationException;
import com.ctg.model.ErrorField;
import com.ctg.repository.UserRepository;
import com.ctg.dto.UserDto;
import com.ctg.mapper.UserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    @Override
    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> {
                   log.error("User not found with id {}", id);
                   return new ResourceNotFoundException("User not found with id " + id);
                });
    }

    @Override
    public UserDto createUser(@Valid UserDto userDto) {
        if (userRepository.existsByEmail(userDto.getEmail())) {
            log.error("Email already exists {}", userDto.getEmail());
            throw new ValidationException(List.of(new ErrorField("email", "Email already exists")));
        }
        var newUser = userRepository.save(userMapper.toEntity(userDto));
        log.debug("Created new user {}", newUser);

        return userMapper.toDto(newUser);
    }

    @Override
    public UserDto updateUser(@Valid UserDto userDto, Long id) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    if (!existingUser.getEmail().equals(userDto.getEmail())) {
                        if (userRepository.existsByEmail(userDto.getEmail())) {
                            log.error("Email already exists {}", userDto.getEmail());
                            throw new ValidationException(List.of(new ErrorField("email", "Email already exists")));
                        }
                    }
                    userMapper.updateEntityFromDto(userDto, existingUser);
                    var updatedUser = userRepository.save(existingUser);
                    log.debug("Newly updated user {}", updatedUser);

                    return userMapper.toDto(updatedUser);

                }).orElseThrow(() -> {
                    log.error("User not found with id {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            log.error("User not found with id {}", id);
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
        log.debug("User is deleted by id {}", id);
    }

    @Transactional(readOnly = true)
    @Override
    public PagedResponse<UserDto> getPagedUsers(int pageNo, int pageSize, String sortBy, String sortDir) {
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
}
