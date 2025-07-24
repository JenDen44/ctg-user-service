package com.ctg.service;

import com.ctg.dto.PagedResponse;
import com.ctg.repository.UserRepository;
import com.ctg.dto.UserDto;
import com.ctg.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        return userMapper.toDto(userRepository.findById(id).get());
    }

    @Override
    public UserDto createUser(UserDto userDto) {
        var savedUser = userRepository.save(userMapper.toEntity(userDto));
        return userMapper.toDto(savedUser);
    }

    @Override
    public UserDto updateUser(UserDto userDto, Long id) {
        userRepository.save(userMapper.toEntity(userDto));
        return userDto;
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public PagedResponse<UserDto> getPagedUsers(int pageNo, int pageSize, String sortBy, String sortDir) {
        var direction = Sort.Direction.fromOptionalString(sortDir).orElse(Sort.Direction.ASC);
        var sort = Sort.by(direction, sortBy);
        var pageRequest = PageRequest.of(pageNo, pageSize, sort);

        var pagedUsers = userRepository.findAll(pageRequest);

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
