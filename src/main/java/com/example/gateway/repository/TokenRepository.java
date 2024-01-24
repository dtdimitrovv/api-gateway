package com.example.gateway.repository;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Configuration
@Repository
public class TokenRepository {
    private final ValueOperations<String, Long> valueOperations;

    public TokenRepository(RedisTemplate<String, Long> redisTemplate) {
        this.valueOperations = redisTemplate.opsForValue();
    }

    public boolean isBlacklisted(String token) {
        return Optional.ofNullable(this.valueOperations.get(token))
                .map(cacheToken -> cacheToken.equals(-1L))
                .orElse(Boolean.FALSE);
    }

    public Long getId(String token) {
        return this.valueOperations.get(token);
    }

    public void cache(String token, Long id) {
        this.valueOperations.set(token, id, Duration.ofMinutes(30));
    }

    public void blacklist(String token) {
        this.valueOperations.set(token, -1L);
    }
}
