package com.ctg.dto;

import com.ctg.model.Role;
import lombok.Builder;

@Builder
public record LoginUserResponse(
        Long id, String email, String passwordHash, Role role, int tokenVersion
) {}
