package com.sara.tfgdam.security;

import com.sara.tfgdam.domain.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

@Service
public class JwtTokenService {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;

    public JwtTokenService(@Value("${sara.jwt.secret}") String jwtSecret,
                           @Value("${sara.jwt.access-token-expiration-ms:3600000}") long accessTokenExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public String generateAccessToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(accessTokenExpirationMs);

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("roles", principal.getRoles().stream().map(UserRole::name).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserPrincipal user) {
        String username = extractUsername(token);
        Date expiration = parseClaims(token).getExpiration();
        return username.equalsIgnoreCase(user.getUsername()) && expiration.after(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
}
