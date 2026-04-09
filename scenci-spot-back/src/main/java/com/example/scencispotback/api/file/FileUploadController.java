package com.example.scencispotback.api.file;

import com.example.scencispotback.common.ApiResponse;
import com.example.scencispotback.common.BizException;
import com.example.scencispotback.security.Authz;
import com.example.scencispotback.service.AliOssService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/files")
public class FileUploadController {

    private final AliOssService aliOssService;

    public FileUploadController(AliOssService aliOssService) {
        this.aliOssService = aliOssService;
    }

    @PostMapping("/image/upload")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        Authz.requireRole("ADMIN");
        String url = aliOssService.uploadImage(file);
        return ApiResponse.ok(Map.of("url", url));
    }

    @PostMapping(value = "/video/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> uploadVideo(@RequestParam("file") MultipartFile file) {
        Authz.requireRole("ADMIN");
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }
        String name = file.getOriginalFilename() == null ? "video.mp4" : file.getOriginalFilename();
        String lower = name.toLowerCase(Locale.ROOT);
        if (!(lower.endsWith(".mp4") || lower.endsWith(".avi") || lower.endsWith(".mov") || lower.endsWith(".mkv") || lower.endsWith(".flv"))) {
            throw new BizException("仅支持 mp4/avi/mov/mkv/flv 视频文件");
        }
        try {
            LocalDate now = LocalDate.now();
            Path base = Path.of(System.getProperty("user.home"), "scenic-spot", "videos",
                String.valueOf(now.getYear()), String.format("%02d", now.getMonthValue()), String.format("%02d", now.getDayOfMonth()));
            Files.createDirectories(base);
            String ext = lower.substring(lower.lastIndexOf('.'));
            Path target = base.resolve(UUID.randomUUID().toString().replace("-", "") + ext);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return ApiResponse.ok(Map.of("path", target.toAbsolutePath().toString()));
        } catch (IOException e) {
            throw new BizException("视频上传失败: " + e.getMessage());
        }
    }
}
