package com.example.scencispotback.security;

// 登录用户信息
public record LoginUser(Long userId, String role, Long scenicId, String nickname) {
}
