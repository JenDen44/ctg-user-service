package com.ctg.model;

public enum Role {
    SUPER_ADMIN,
    ADMIN,
    EMPLOYEE;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
