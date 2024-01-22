package com.example.gateway.service;

import com.example.gateway.repository.TokenRepository;
import org.springframework.stereotype.Service;

@Service
public record RedisService(TokenRepository tokenRepository) {

    public void cache(String token, Long id) {
        this.tokenRepository.cache(token, id);
    }

    public Long getById(String token) {
        return this.tokenRepository.getId(token);
    }

    public boolean isBlackListed(String token) {
        return this.tokenRepository.isBlacklisted(token);
    }

    public void blackList(String token) {
        this.tokenRepository.blacklist(token);
    }

}
