package com.example.scencispotback.api.auth;

import com.example.scencispotback.common.ApiResponse;
import com.example.scencispotback.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/sendCode")
    public ApiResponse<Map<String, String>> sendCode(@Valid @RequestBody AuthDto.SendCodeReq req) {
        String code = authService.sendCode(req.phone());
        return ApiResponse.ok("模拟短信已发送", Map.of("phone", req.phone(), "mockCode", code));
    }

    @PostMapping("/registerByCode")
    public ApiResponse<AuthDto.LoginResp> registerByCode(@Valid @RequestBody AuthDto.RegisterByCodeReq req) {
        return ApiResponse.ok(authService.registerByCode(req));
    }

    @PostMapping("/loginByCode")
    public ApiResponse<AuthDto.LoginResp> loginByCode(@Valid @RequestBody AuthDto.LoginByCodeReq req) {
        return ApiResponse.ok(authService.loginByCode(req));
    }

    @PostMapping("/loginByPassword")
    public ApiResponse<AuthDto.LoginResp> loginByPassword(@Valid @RequestBody AuthDto.LoginByPasswordReq req) {
        return ApiResponse.ok(authService.loginByPassword(req));
    }

    @PostMapping("/loginByOAuthMock")
    public ApiResponse<AuthDto.LoginResp> loginByOAuthMock(@Valid @RequestBody AuthDto.OAuthMockReq req) {
        return ApiResponse.ok(authService.loginByOAuthMock(req));
    }

    @PostMapping("/loginByWechatMini")
    public ApiResponse<AuthDto.LoginResp> loginByWechatMini(@Valid @RequestBody AuthDto.WechatMiniLoginReq req) {
        return ApiResponse.ok(authService.loginByWechatMini(req));
    }

    @GetMapping("/me")
    public ApiResponse<AuthDto.LoginResp> me() {
        return ApiResponse.ok(authService.me());
    }
}
