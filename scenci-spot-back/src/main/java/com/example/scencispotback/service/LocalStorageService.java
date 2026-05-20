package com.example.scencispotback.service;

import com.example.scencispotback.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
// 本地文件存储服务
public class LocalStorageService {

    private final String localBaseDir;
    private final String publicBaseUrl;

    public LocalStorageService(@Value("${app.storage.local-base-dir:${user.home}/scenic-spot/uploads}") String localBaseDir,
                               @Value("${app.storage.public-base-url:http://39.96.52.107:8080/uploads}") String publicBaseUrl) {
        this.localBaseDir = localBaseDir;
        this.publicBaseUrl = publicBaseUrl;
    }

    /**
     * 上传图片并返回公网URL。
     */
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BizException("仅支持图片文件上传");
        }

        String objectKey = buildObjectKey(file.getOriginalFilename());
        Path target = Path.of(localBaseDir, objectKey).toAbsolutePath().normalize();

        try (InputStream inputStream = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            return joinUrl(publicBaseUrl, objectKey);
        } catch (IOException e) {
            throw new BizException("读取上传文件失败: " + e.getMessage());
        } catch (Exception e) {
            throw new BizException("保存本地图片失败: " + e.getMessage());
        }
    }

    private String buildObjectKey(String originalFilename) {
        String ext = "jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        LocalDate now = LocalDate.now();
        return "images/"
            + now.getYear() + "/"
            + String.format("%02d", now.getMonthValue()) + "/"
            + String.format("%02d", now.getDayOfMonth()) + "/"
            + UUID.randomUUID().toString().replace("-", "")
            + "."
            + ext;
    }

    private String joinUrl(String base, String key) {
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return normalizedBase + "/" + key.replace('\\', '/');
    }
}
