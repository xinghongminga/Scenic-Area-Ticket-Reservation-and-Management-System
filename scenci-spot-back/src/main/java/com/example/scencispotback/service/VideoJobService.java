package com.example.scencispotback.service;

import com.example.scencispotback.api.video.VideoJobDto;
import com.example.scencispotback.common.BizException;
import com.example.scencispotback.domain.VideoAnalysisJob;
import com.example.scencispotback.mapper.FlowAreaMinuteMapper;
import com.example.scencispotback.mapper.FlowMinuteMapper;
import com.example.scencispotback.mapper.VideoAnalysisJobMapper;
import com.example.scencispotback.mapper.VideoPeopleCountMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.scencispotback.security.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 视频客流分析任务服务
 */
@Service
public class VideoJobService {

    private final VideoAnalysisJobMapper jobMapper;
    private final VideoPeopleCountMapper videoPeopleCountMapper;
    private final FlowAreaMinuteMapper flowAreaMinuteMapper;
    private final FlowMinuteMapper flowMinuteMapper;
    private final ObjectMapper objectMapper;

    // 是否开启视频检测
    @Value("${app.video-detection.enabled:false}")
    private boolean videoDetectionEnabled;

    // Python检测服务地址
    @Value("${app.video-detection.base-url:http://127.0.0.1:5001}")
    private String videoDetectionBaseUrl;

    // 接口超时时间
    @Value("${app.video-detection.timeout-ms:120000}")
    private Integer videoDetectionTimeoutMs;

    // 抽帧步长
    @Value("${app.video-detection.frame-step:5}")
    private Integer videoDetectionFrameStep;

    // 异步线程数
    @Value("${app.video-detection.worker-threads:2}")
    private Integer videoDetectionWorkerThreads;

    // 检测任务线程池
    private volatile ExecutorService detectionExecutor;

    public VideoJobService(VideoAnalysisJobMapper jobMapper,
                           VideoPeopleCountMapper videoPeopleCountMapper,
                           FlowAreaMinuteMapper flowAreaMinuteMapper,
                           FlowMinuteMapper flowMinuteMapper,
                           ObjectMapper objectMapper) {
        this.jobMapper = jobMapper;
        this.videoPeopleCountMapper = videoPeopleCountMapper;
        this.flowAreaMinuteMapper = flowAreaMinuteMapper;
        this.flowMinuteMapper = flowMinuteMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建视频分析任务
     */
    @Transactional
    public Long create(VideoJobDto.CreateReq req) {
        VideoAnalysisJob job = new VideoAnalysisJob();
        job.setScenicId(req.scenicId());
        job.setVideoPath(req.videoPath());
        job.setAreaCode(req.areaCode());
        job.setDirection(normalizeDirection(req.direction()));
        job.setSampleMs(req.sampleMs());
        job.setStatus("PENDING");
        job.setCreatedBy(UserContext.get().userId());
        jobMapper.insert(job);
        return job.getId();
    }

    /**
     * 查询所有任务
     */
    public List<VideoJobDto.JobResp> list() {
        return jobMapper.listAll().stream().map(this::toResp).toList();
    }

    /**
     * 删除任务，运行中不可删
     */
    @Transactional
    public void delete(Long id) {
        VideoAnalysisJob job = jobMapper.findById(id);
        if (job == null) {
            throw new BizException("视频任务不存在");
        }
        if ("RUNNING".equals(job.getStatus())) {
            throw new BizException("任务运行中，暂不允许删除");
        }
        videoPeopleCountMapper.deleteByJobId(id);
        jobMapper.deleteById(id);
    }

    /**
     * 启动执行视频检测任务
     */
    public VideoJobDto.RunResp run(Long id) {
        VideoAnalysisJob job = jobMapper.findById(id);
        if (job == null) {
            throw new BizException("视频任务不存在");
        }
        LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(10);
        if ("RUNNING".equals(job.getStatus())
                && job.getUpdatedAt() != null
                && job.getUpdatedAt().isAfter(staleBefore)) {
            throw new BizException("任务正在运行中");
        }
        int claimed = jobMapper.claimForRunningOrStale(id, staleBefore);
        if (claimed == 0) {
            VideoAnalysisJob latest = jobMapper.findById(id);
            if (latest == null) {
                throw new BizException("视频任务不存在");
            }
            if ("SUCCESS".equals(latest.getStatus())) {
                throw new BizException("该任务已执行成功。为避免重复累计统计，请新建任务后再执行");
            }
            if ("RUNNING".equals(latest.getStatus())) {
                throw new BizException("任务正在运行中");
            }
            throw new BizException("任务当前状态为 " + latest.getStatus() + "，暂不可执行");
        }

        try {
            getDetectionExecutor().submit(() -> executeJob(id));
        } catch (Exception ex) {
            jobMapper.updateStatus(id, "FAILED", "任务提交失败: " + ex.getMessage());
            throw new BizException("任务提交失败: " + ex.getMessage());
        }

        return new VideoJobDto.RunResp(id, "RUNNING", 0, 0);
    }

    /**
     * 异步执行视频检测核心逻辑
     */
    private void executeJob(Long id) {
        VideoAnalysisJob job = jobMapper.findById(id);
        if (job == null) {
            return;
        }

        try {
            Path video = Path.of(job.getVideoPath());
            if (!Files.exists(video) || !Files.isRegularFile(video)) {
                throw new BizException("本地视频文件不存在: " + job.getVideoPath());
            }

            long size = Files.size(video);
            int sampleMs = job.getSampleMs() == null ? 1000 : job.getSampleMs();
            String areaCode = (job.getAreaCode() == null || job.getAreaCode().isBlank()) ? "MAIN" : job.getAreaCode();
            String direction = normalizeDirection(job.getDirection());

            List<DetectionPoint> detectionPoints = detectPeople(job, areaCode, direction, sampleMs, size);
            if (detectionPoints.isEmpty()) {
                throw new BizException("检测结果为空，请检查视频内容或调整检测参数");
            }

            Map<LocalDateTime, Integer> minuteCount = new LinkedHashMap<>();
            for (DetectionPoint point : detectionPoints) {
                LocalDateTime statTime = point.statTime();
                int count = Math.max(0, point.peopleCount());
                if (count <= 0) {
                    continue;
                }

                // 存入 逐秒/逐采样点 原始人数表
                videoPeopleCountMapper.insert(job.getId(), job.getScenicId(), areaCode, statTime, count);
                // 把秒级时间转为整分钟，同分钟内人数累加
                LocalDateTime minute = statTime.withSecond(0).withNano(0);
                minuteCount.merge(minute, count, Integer::sum);
            }

            //  将分钟聚合后的数据，更新到区域客流、全局客流表
            for (Map.Entry<LocalDateTime, Integer> e : minuteCount.entrySet()) {
                int cnt = e.getValue();
                // 更新【区域分钟客流】
                flowAreaMinuteMapper.upsert(job.getScenicId(), areaCode, e.getKey(), cnt);
                // 区分进出方向更新【全局分钟客流】
                if ("EXIT".equals(direction)) {
                    flowMinuteMapper.upsertOut(job.getScenicId(), e.getKey(), cnt);
                } else {
                    flowMinuteMapper.upsertIn(job.getScenicId(), e.getKey(), cnt, cnt);
                }
            }

            jobMapper.updateStatus(id, "SUCCESS", null);
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            if (errorMsg != null && errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500);
            }
            jobMapper.updateStatus(id, "FAILED", errorMsg);
        }
    }

    /**
     * 获取线程池，懒加载
     */
    private ExecutorService getDetectionExecutor() {
        if (detectionExecutor == null) {
            synchronized (this) {
                if (detectionExecutor == null) {
                    int workers = (videoDetectionWorkerThreads == null || videoDetectionWorkerThreads <= 0) ? 2 : videoDetectionWorkerThreads;
                    detectionExecutor = Executors.newFixedThreadPool(workers);
                }
            }
        }
        return detectionExecutor;
    }

    /**
     * 实体转返回DTO
     */
    private VideoJobDto.JobResp toResp(VideoAnalysisJob job) {
        return new VideoJobDto.JobResp(job.getId(), job.getScenicId(), job.getVideoPath(), job.getAreaCode(),
                job.getDirection(), job.getSampleMs(), job.getStatus(), job.getErrorMsg(), job.getCreatedAt());
    }

    /**
     * 调用Python服务进行人数检测
     */
    private List<DetectionPoint> detectPeople(VideoAnalysisJob job,
                                              String areaCode,
                                              String direction,
                                              int sampleMs,
                                              long fileSize) {
        if (!videoDetectionEnabled) {
            throw new BizException("视频真检测未启用，请先在配置中开启 app.video-detection.enabled=true");
        }

        String endpoint = normalizeBaseUrl(videoDetectionBaseUrl) + "/api/detect/people";
        Map<String, Object> payload = Map.of(
                "jobId", job.getId(),
                "scenicId", job.getScenicId(),
                "videoPath", job.getVideoPath(),
                "areaCode", areaCode,
                "direction", direction,
                "sampleMs", sampleMs,
                "fileSize", fileSize,
                "frameStep", videoDetectionFrameStep == null ? 5 : videoDetectionFrameStep
        );

        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpURLConnection connection = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(videoDetectionTimeoutMs == null ? 120000 : videoDetectionTimeoutMs);
            connection.setReadTimeout(videoDetectionTimeoutMs == null ? 120000 : videoDetectionTimeoutMs);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(bytes);
                outputStream.flush();
            }

            int statusCode = connection.getResponseCode();
            String responseBody = readResponseBody(connection, statusCode);
            if (statusCode < 200 || statusCode >= 300) {
                throw new BizException("调用Python检测服务失败，HTTP " + statusCode + "，响应: " + responseBody);
            }
            return parseDetectionPoints(responseBody);
        } catch (IOException e) {
            throw new BizException("调用Python检测服务异常: " + e.getMessage());
        } catch (RuntimeException e) {
            throw new BizException("调用Python检测服务异常: " + e.getMessage());
        }
    }

    /**
     * 解析检测结果
     */
    private List<DetectionPoint> parseDetectionPoints(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode dataNode = root.has("data") ? root.get("data") : root;
            JsonNode pointsNode = dataNode.get("points");
            if (pointsNode == null || !pointsNode.isArray()) {
                throw new BizException("Python检测服务返回格式错误：缺少 points 数组");
            }

            List<DetectionPoint> points = new ArrayList<>();
            for (JsonNode node : pointsNode) {
                String statTimeRaw = node.path("statTime").asText(null);
                int peopleCount = node.path("peopleCount").asInt(0);
                if (statTimeRaw == null || statTimeRaw.isBlank()) {
                    continue;
                }
                points.add(new DetectionPoint(parseStatTime(statTimeRaw), peopleCount));
            }
            return points;
        } catch (IOException e) {
            throw new BizException("解析Python检测结果失败: " + e.getMessage());
        }
    }

    /**
     * 兼容多种时间格式解析
     */
    private LocalDateTime parseStatTime(String raw) {
        try {
            return LocalDateTime.parse(raw);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(raw).toLocalDateTime();
            } catch (DateTimeParseException ignoredAgain) {
                return LocalDateTime.ofInstant(Instant.parse(raw), ZoneId.systemDefault());
            }
        }
    }

    /**
     * 处理服务地址，去除末尾斜杠
     */
    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BizException("请配置 app.video-detection.base-url");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * 统一进出方向字段
     */
    private String normalizeDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return "ENTER";
        }
        String d = direction.trim().toUpperCase();
        if ("EXIT".equals(d) || "OUT".equals(d) || "LEAVE".equals(d)) {
            return "EXIT";
        }
        return "ENTER";
    }

    /**
     * 读取接口响应内容
     */
    private String readResponseBody(HttpURLConnection connection, int statusCode) throws IOException {
        InputStream inputStream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (inputStream == null) {
            return "";
        }
        try (InputStream stream = inputStream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // 检测点记录：时间+人数
    private record DetectionPoint(LocalDateTime statTime, int peopleCount) {}
}