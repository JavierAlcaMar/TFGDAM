package com.sara.tfgdam.dto;

import com.sara.tfgdam.domain.entity.UserRole;
import com.sara.tfgdam.domain.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class CreateUserRequest {

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    private String email;

    @NotBlank(message = "password is required")
    private String password;

    private Set<UserRole> roles;

    private UserStatus status;
}
