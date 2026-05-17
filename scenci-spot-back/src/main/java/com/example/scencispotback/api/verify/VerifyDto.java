package com.example.scencispotback.api.verify;

import jakarta.validation.constraints.NotBlank;

// 验证码 DTO
public class VerifyDto {
    public record VerifyByCodeReq(@NotBlank String verifyCode, String method) {}

    public record VerifyByQrReq(@NotBlank String qrCode, String method) {}

    public record VerifyByQrImageReq(String method) {}

    public record VerifyResp(String orderNo, String ticketStatus, String orderStatus) {}
}
