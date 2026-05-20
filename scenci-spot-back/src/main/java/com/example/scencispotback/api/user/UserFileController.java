package com.example.scencispotback.api.user;

import com.example.scencispotback.common.ApiResponse;
import com.example.scencispotback.security.Authz;
import com.example.scencispotback.service.LocalStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
// 用户文件控制器
public class UserFileController {

    private final LocalStorageService localStorageService;

    public UserFileController(LocalStorageService localStorageService) {
        this.localStorageService = localStorageService;
    }

    @PostMapping("/avatar/upload")
    /**
     * 上传用户头像并返回可访问URL。
     */
    public ApiResponse<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Authz.requireRole("TOURIST", "ADMIN", "ANALYST", "AUDITOR");
        String url = localStorageService.uploadImage(file);
        return ApiResponse.ok(Map.of("url", url));
    }
}
