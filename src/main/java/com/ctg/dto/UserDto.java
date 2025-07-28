package com.ctg.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ctg.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {

    private Long id;

    @NotBlank(message = "Full name is mandatory")
    @Size(min = 2, max = 100, message = "Full name must be 2-100 characters")
    private String fullName;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    private String email;


    @NotBlank(message = "Password is mandatory")
    @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @NotNull(message = "Role is mandatory")
    private Role role;
}
