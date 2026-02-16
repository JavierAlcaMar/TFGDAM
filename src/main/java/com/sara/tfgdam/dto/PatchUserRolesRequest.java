package com.sara.tfgdam.dto;

import com.sara.tfgdam.domain.entity.UserRole;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class PatchUserRolesRequest {

    @NotEmpty(message = "roles cannot be empty")
    private Set<UserRole> roles;
}
