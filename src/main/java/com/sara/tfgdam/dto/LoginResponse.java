package com.sara.tfgdam.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class LoginResponse {
    String accessToken;
    String tokenType;
    Long userId;
    String email;
    Set<String> roles;
    String status;
}
