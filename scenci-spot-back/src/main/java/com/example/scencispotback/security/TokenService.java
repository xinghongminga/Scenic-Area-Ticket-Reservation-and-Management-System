package com.example.scencispotback.security;

import com.example.scencispotback.common.BizException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
// 令牌服务
public class TokenService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final long tokenTtlSeconds;

    public TokenService(StringRedisTemplate redisTemplate,
                        ObjectMapper objectMapper,
                        @Value("${app.auth.token-ttl-seconds}") long tokenTtlSeconds) {
        this.redisTemplate = redisTemplate;
         this.objectMapper = objectMapper;
        this.tokenTtlSeconds = tokenTtlSeconds;
    }

    public String issueToken(LoginUser loginUser) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = tokenKey(token);
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(loginUser), Duration.ofSeconds(tokenTtlSeconds));
            return token;
        } catch (JsonProcessingException e) {
            throw new BizException("生成令牌失败");
        }
    }

    public LoginUser parseToken(String token) {
        String value = redisTemplate.opsForValue().get(tokenKey(token));
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, LoginUser.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String tokenKey(String token) {
        return "auth:token:" + token;
    }
}
