package com.example.scencispotback.api.verify;

import com.example.scencispotback.common.ApiResponse;
import com.example.scencispotback.security.Authz;
import com.example.scencispotback.service.VerifyService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/byQrImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<VerifyDto.VerifyResp> byQrImage(@RequestParam("file") MultipartFile file,
                                                       @RequestParam(value = "method", required = false) String method) {
        Authz.requireRole("ADMIN", "AUDITOR");
        return ApiResponse.ok(verifyService.verifyByQrImage(file, method));
    }
}
