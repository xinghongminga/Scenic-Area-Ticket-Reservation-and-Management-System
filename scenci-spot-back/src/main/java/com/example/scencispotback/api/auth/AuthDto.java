package com.example.scencispotback.api.auth;

import jakarta.validation.constraints.NotBlank;

public class AuthDto {

    public record SendCodeReq(@NotBlank String phone) {}

    public record RegisterByCodeReq(@NotBlank String phone,
                                    @NotBlank String code,
                                    String nickname,
                                    @NotBlank String fullName,
                                    @NotBlank String idCardNo,
                                    String username,
                                    String password) {}

    public record LoginByCodeReq(@NotBlank String phone, @NotBlank String code, String nickname) {}

    public record LoginByPasswordReq(@NotBlank String username, @NotBlank String password, String role) {}

    public record OAuthMockReq(@NotBlank String provider, @NotBlank String mockOpenId, String nickname) {}

    public record WechatMiniLoginReq(@NotBlank String code, String nickname, String avatarUrl, String devOpenId) {}

    public record LoginResp(String token, Long userId, String role, String nickname, String avatarUrl) {}
}
