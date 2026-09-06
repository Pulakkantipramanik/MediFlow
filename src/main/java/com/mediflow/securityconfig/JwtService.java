package com.mediflow.securityconfig;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    // PURPOSE:
    // Read the JWT secret from application.properties.
    //
    // WHY:
    // Keeping the secret outside the Java source code
    // prevents sensitive credentials from being hardcoded
    // into the application.
    @Value("${jwt.secret}")
    private String secretKey;

    // PURPOSE:
    // Defines how long a JWT remains valid.
    //
    // CURRENT BUSINESS RULE:
    // Token expires after 1 hour.
    private final long EXPIRATION_TIME =
            1000 * 60 * 60; // 1 hour

    // PURPOSE:
    // Creates the cryptographic signing key from the configured secret.
    //
    // WHY:
    // The same signing key must be used both when generating
    // and validating JWT tokens.
    private SecretKey getSigningKey() {

        return Keys.hmacShaKeyFor(
                secretKey.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(String email, String role) {

        return Jwts.builder()

                .subject(email)

                .claim("role", role)

                .issuedAt(new Date())

                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + EXPIRATION_TIME
                        )
                )

                .signWith(getSigningKey())

                .compact();
    }

    public String extractEmail(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isTokenValid(String token) {

        try {

            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);

            return true;

        } catch (Exception e) {

            return false;
        }
    }
}
