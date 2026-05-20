package com.example.scencispotback.api.auth;

import com.example.scencispotback.common.ApiResponse;
import com.example.scencispotback.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
// 认证控制器
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/sendCode")
    /**
     * 发送短信验证码（模拟）。
     */
    public ApiResponse<Map<String, String>> sendCode(@Valid @RequestBody AuthDto.SendCodeReq req) {
        String code = authService.sendCode(req.phone());
        return ApiResponse.ok("模拟短信已发送", Map.of("phone", req.phone(), "mockCode", code));
    }

    @PostMapping("/registerByCode")
    /**
     * 使用验证码注册账号。
     */
    public ApiResponse<AuthDto.LoginResp> registerByCode(@Valid @RequestBody AuthDto.RegisterByCodeReq req) {
        return ApiResponse.ok(authService.registerByCode(req));
    }

    @PostMapping("/loginByCode")
    /**
     * 使用验证码登录。
     */
    public ApiResponse<AuthDto.LoginResp> loginByCode(@Valid @RequestBody AuthDto.LoginByCodeReq req) {
        return ApiResponse.ok(authService.loginByCode(req));
    }

    @PostMapping("/loginByPassword")
    /**
     * 使用账号密码登录。
     */
    public ApiResponse<AuthDto.LoginResp> loginByPassword(@Valid @RequestBody AuthDto.LoginByPasswordReq req) {
        return ApiResponse.ok(authService.loginByPassword(req));
    }

    @PostMapping("/loginByOAuthMock")
    /**
     * 模拟第三方登录。
     */
    public ApiResponse<AuthDto.LoginResp> loginByOAuthMock(@Valid @RequestBody AuthDto.OAuthMockReq req) {
        return ApiResponse.ok(authService.loginByOAuthMock(req));
    }

    @PostMapping("/loginByWechatMini")
    /**
     * 微信小程序登录。
     */
    public ApiResponse<AuthDto.LoginResp> loginByWechatMini(@Valid @RequestBody AuthDto.WechatMiniLoginReq req) {
        return ApiResponse.ok(authService.loginByWechatMini(req));
    }

    @GetMapping("/me")
    /**
     * 获取当前登录用户信息。
     */
    public ApiResponse<AuthDto.LoginResp> me() {
        return ApiResponse.ok(authService.me());
    }
}
