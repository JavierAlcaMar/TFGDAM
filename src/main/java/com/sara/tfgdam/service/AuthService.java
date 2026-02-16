package com.sara.tfgdam.service;

import com.sara.tfgdam.dto.LoginRequest;
import com.sara.tfgdam.dto.LoginResponse;
import com.sara.tfgdam.security.JwtTokenService;
import com.sara.tfgdam.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().trim().toLowerCase(),
                        request.getPassword()
                )
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        return LoginResponse.builder()
                .accessToken(jwtTokenService.generateAccessToken(principal))
                .tokenType("Bearer")
                .userId(principal.getId())
                .email(principal.getEmail())
                .roles(principal.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()))
                .status(principal.getStatus().name())
                .build();
    }
}
