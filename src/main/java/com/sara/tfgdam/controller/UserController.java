package com.sara.tfgdam.controller;

import com.sara.tfgdam.dto.CreateUserRequest;
import com.sara.tfgdam.dto.PatchUserRolesRequest;
import com.sara.tfgdam.dto.UserResponse;
import com.sara.tfgdam.mapper.DtoMapper;
import com.sara.tfgdam.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementService userManagementService;
    private final DtoMapper mapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return mapper.toUserResponse(userManagementService.createUser(request));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTOR')")
    public UserResponse activateUser(@PathVariable Long id) {
        return mapper.toUserResponse(userManagementService.activateUser(id));
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTOR')")
    public UserResponse disableUser(@PathVariable Long id) {
        return mapper.toUserResponse(userManagementService.disableUser(id));
    }

    @PatchMapping("/{id}/roles")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public UserResponse patchRoles(@PathVariable Long id, @Valid @RequestBody PatchUserRolesRequest request) {
        return mapper.toUserResponse(userManagementService.patchRoles(id, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN','DIRECTOR')")
    public List<UserResponse> listUsers() {
        return userManagementService.listUsers().stream()
                .map(mapper::toUserResponse)
                .toList();
    }

    @GetMapping("/me")
    public UserResponse me() {
        return mapper.toUserResponse(userManagementService.getCurrentUser());
    }
}
