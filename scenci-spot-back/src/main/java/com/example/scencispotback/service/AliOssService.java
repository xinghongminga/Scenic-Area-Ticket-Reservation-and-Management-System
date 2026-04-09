package com.example.scencispotback.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.example.scencispotback.common.BizException;
import com.example.scencispotback.config.AliOssProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
public class AliOssService {

    private final AliOssProperties aliOssProperties;

    public AliOssService(AliOssProperties aliOssProperties) {
        this.aliOssProperties = aliOssProperties;
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

        // 上传前校验 OSS 关键配置
        validateOssConfig();

        // 生成按日期分层的对象 Key，便于后续按目录管理
        String objectKey = buildObjectKey(file.getOriginalFilename());

        // 根据配置构建 OSS 客户端实例
        OSS ossClient = new OSSClientBuilder().build(
            aliOssProperties.getEndpoint(),
            aliOssProperties.getAccessKeyId(),
            aliOssProperties.getAccessKeySecret()
        );

        // 上传对象并返回可直接访问的公网 URL
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(file.getSize());
            ossClient.putObject(aliOssProperties.getBucketName(), objectKey, inputStream, metadata);
            return "https://" + aliOssProperties.getBucketName() + "." + aliOssProperties.getEndpoint() + "/" + objectKey;
        } catch (IOException e) {
            // 读取上传流失败
            throw new BizException("读取上传文件失败: " + e.getMessage());
        } catch (Exception e) {
            // OSS SDK 调用失败
            throw new BizException("上传到阿里云OSS失败: " + e.getMessage());
        } finally {
            // 及时释放客户端资源
            ossClient.shutdown();
        }
    }

    private void validateOssConfig() {
        if (isBlank(aliOssProperties.getEndpoint())
            || isBlank(aliOssProperties.getAccessKeyId())
            || isBlank(aliOssProperties.getAccessKeySecret())
            || isBlank(aliOssProperties.getBucketName())) {
            throw new BizException("OSS配置不完整，请检查配置或启用dev配置");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String buildObjectKey(String originalFilename) {
        String ext = "jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        LocalDate now = LocalDate.now();
        return "scenic-spot/"
            + now.getYear() + "/"
            + String.format("%02d", now.getMonthValue()) + "/"
            + String.format("%02d", now.getDayOfMonth()) + "/"
            + UUID.randomUUID().toString().replace("-", "")
            + "."
            + ext;
    }
}
