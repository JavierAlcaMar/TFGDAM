package com.sara.tfgdam.dto;

import com.sara.tfgdam.domain.entity.UserRole;
import com.sara.tfgdam.domain.entity.UserStatus;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.Set;

@Value
@Builder
public class UserResponse {
    Long id;
    String email;
    Set<UserRole> roles;
    UserStatus status;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
