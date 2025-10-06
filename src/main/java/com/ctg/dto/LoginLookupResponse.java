package com.ctg.dto;

import com.ctg.model.Role;

public record LoginLookupResponse(
        Long id, String email, String passwordHash, Role role, int tokenVersion
) {}
