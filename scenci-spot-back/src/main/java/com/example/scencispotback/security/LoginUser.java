package com.example.scencispotback.security;

public record LoginUser(Long userId, String role, Long scenicId, String nickname) {
}
