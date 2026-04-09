package com.example.scencispotback.api.verify;

import com.example.scencispotback.common.ApiResponse;
import com.example.scencispotback.security.Authz;
import com.example.scencispotback.service.VerifyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/verify")
public class VerifyController {

    private final VerifyService verifyService;

    public VerifyController(VerifyService verifyService) {
        this.verifyService = verifyService;
    }

    @PostMapping("/byCode")
    public ApiResponse<VerifyDto.VerifyResp> byCode(@Valid @RequestBody VerifyDto.VerifyByCodeReq req) {
        Authz.requireRole("ADMIN", "AUDITOR");
        return ApiResponse.ok(verifyService.verifyByCode(req));
    }

    @PostMapping("/byQr")
    public ApiResponse<VerifyDto.VerifyResp> byQr(@Valid @RequestBody VerifyDto.VerifyByQrReq req) {
        Authz.requireRole("ADMIN", "AUDITOR");
        return ApiResponse.ok(verifyService.verifyByQr(req));
    }
}
