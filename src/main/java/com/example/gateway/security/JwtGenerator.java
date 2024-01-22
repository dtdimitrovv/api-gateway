package com.example.gateway.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtGenerator {

    private final String secret;

    public JwtGenerator(
            @Value("${security.token.secret}") String secret)
    {
        this.secret = secret;
    }

    public DecodedJWT decodeToken(String token) {
        return JWT.require(Algorithm.HMAC256(secret))
                .build()
                .verify(token);
    }

}
