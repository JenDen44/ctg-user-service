package com.ctg.dto;

import com.ctg.model.Role;
import lombok.*;

@Getter
@AllArgsConstructor
@Builder
@ToString
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private Role role;
}
