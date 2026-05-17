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
// OSS 文件服务
public class AliOssService {

    private final String localBaseDir;
    private final String publicBaseUrl;

    public AliOssService(@Value("${app.storage.local-base-dir:${user.home}/scenic-spot/uploads}") String localBaseDir,
                         @Value("${app.storage.public-base-url:http://39.96.52.107:8080/uploads}") String publicBaseUrl) {
        this.localBaseDir = localBaseDir;
        this.publicBaseUrl = publicBaseUrl;
    }

    public String uploadImage(MultipartFile file) {
        // 基础入参校验：文件不能为空
        if (file == null || file.isEmpty()) {
            throw new BizException("上传文件不能为空");
        }

        // 仅允许图片类型，避免非图片文件进入对象存储
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BizException("仅支持图片文件上传");
        }

        // 生成按日期分层的对象 Key，便于后续按目录管理
        String objectKey = buildObjectKey(file.getOriginalFilename());
        Path target = Path.of(localBaseDir, objectKey).toAbsolutePath().normalize();

        // 写入本地文件系统并返回可直接访问 URL
        try (InputStream inputStream = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            return joinUrl(publicBaseUrl, objectKey);
        } catch (IOException e) {
            // 读取上传流失败
            throw new BizException("读取上传文件失败: " + e.getMessage());
        } catch (Exception e) {
            // 本地存储调用失败
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
