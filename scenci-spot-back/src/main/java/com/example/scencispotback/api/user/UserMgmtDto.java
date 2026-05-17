package com.example.scencispotback.api.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

// 用户管理 DTO
public class UserMgmtDto {

    public record UserResp(Long id, String role, Long scenicId, Integer status, String loginType,
                            String phone, String username, String nickname, String fullName, String idCardNo,
                            LocalDateTime createdAt) {}

    public record CreateStaffReq(@NotBlank String role,
                                  Long scenicId,
                                  @NotBlank String username,
                                  @NotBlank String password,
                                  String nickname) {}

    public record UpdateUserReq(String nickname, String fullName, String idCardNo, Long scenicId, String role) {}

    public record ResetPasswordReq(@NotBlank String newPassword) {}

    public record UserStatusReq(@NotNull Integer status) {}

    public record UserQuery(String keyword, String role, Integer status) {}
}
