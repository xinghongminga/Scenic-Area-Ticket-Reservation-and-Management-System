package com.example.scencispotback.service;

import com.example.scencispotback.api.video.VideoJobDto;
import com.example.scencispotback.common.BizException;
import com.example.scencispotback.domain.VideoAnalysisJob;
import com.example.scencispotback.mapper.FlowAreaMinuteMapper;
import com.example.scencispotback.mapper.FlowMinuteMapper;
import com.example.scencispotback.mapper.VideoAnalysisJobMapper;
import com.example.scencispotback.mapper.VideoPeopleCountMapper;
import com.example.scencispotback.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class VideoJobService {

    private final VideoAnalysisJobMapper jobMapper;
    private final VideoPeopleCountMapper videoPeopleCountMapper;
    private final FlowAreaMinuteMapper flowAreaMinuteMapper;
    private final FlowMinuteMapper flowMinuteMapper;

    public VideoJobService(VideoAnalysisJobMapper jobMapper,
                           VideoPeopleCountMapper videoPeopleCountMapper,
                           FlowAreaMinuteMapper flowAreaMinuteMapper,
                           FlowMinuteMapper flowMinuteMapper) {
        this.jobMapper = jobMapper;
        this.videoPeopleCountMapper = videoPeopleCountMapper;
        this.flowAreaMinuteMapper = flowAreaMinuteMapper;
        this.flowMinuteMapper = flowMinuteMapper;
    }

    @Transactional
    public Long create(VideoJobDto.CreateReq req) {
        VideoAnalysisJob job = new VideoAnalysisJob();
        job.setScenicId(req.scenicId());
        job.setVideoPath(req.videoPath());
        job.setAreaCode(req.areaCode());
        job.setDirection(req.direction() == null || req.direction().isBlank() ? "ENTER" : req.direction());
        job.setSampleMs(req.sampleMs());
        job.setStatus("PENDING");
        job.setCreatedBy(UserContext.get().userId());
        jobMapper.insert(job);
        return job.getId();
    }

    public List<VideoJobDto.JobResp> list() {
        return jobMapper.listAll().stream().map(this::toResp).toList();
    }

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

    @Transactional
    public VideoJobDto.RunResp run(Long id) {
        VideoAnalysisJob job = jobMapper.lockById(id);
        if (job == null) {
            throw new BizException("视频任务不存在");
        }
        if ("RUNNING".equals(job.getStatus())) {
            throw new BizException("任务正在运行中");
        }

        jobMapper.updateStatus(id, "RUNNING", null);
        try {
            Path video = Path.of(job.getVideoPath());
            if (!Files.exists(video) || !Files.isRegularFile(video)) {
                throw new BizException("本地视频文件不存在: " + job.getVideoPath());
            }

            long size = Files.size(video);
            int sampleMs = job.getSampleMs() == null ? 1000 : job.getSampleMs();
            int durationSec = (int) Math.max(60, Math.min(600, size / 50000));
            int points = Math.max(1, Math.min(1000, durationSec * 1000 / sampleMs));
            LocalDateTime start = LocalDateTime.now().minusSeconds(durationSec);
            String areaCode = (job.getAreaCode() == null || job.getAreaCode().isBlank()) ? "MAIN" : job.getAreaCode();
            String direction = (job.getDirection() == null || job.getDirection().isBlank()) ? "ENTER" : job.getDirection();

            Random random = new Random((job.getVideoPath() + "#" + job.getId()).hashCode());
            Map<LocalDateTime, Integer> minuteCount = new LinkedHashMap<>();
            for (int i = 0; i < points; i++) {
                LocalDateTime statTime = start.plusNanos((long) i * sampleMs * 1_000_000L);
                int count = Math.max(1, random.nextInt(4) + 1);  // simulate 1-4 people per sample

                videoPeopleCountMapper.insert(job.getId(), job.getScenicId(), areaCode, statTime, count);
                LocalDateTime minute = statTime.withSecond(0).withNano(0);
                minuteCount.merge(minute, count, Integer::sum);
            }

            for (Map.Entry<LocalDateTime, Integer> e : minuteCount.entrySet()) {
                int cnt = e.getValue();
                flowAreaMinuteMapper.upsert(job.getScenicId(), areaCode, e.getKey(), cnt);
                if ("EXIT".equals(direction)) {
                    flowMinuteMapper.upsertOut(job.getScenicId(), e.getKey(), cnt);
                } else {
                    flowMinuteMapper.upsertIn(job.getScenicId(), e.getKey(), cnt, cnt);
                }
            }

            jobMapper.updateStatus(id, "SUCCESS", null);
            return new VideoJobDto.RunResp(id, "SUCCESS", points, minuteCount.size());
        } catch (Exception ex) {
            jobMapper.updateStatus(id, "FAILED", ex.getMessage());
            if (ex instanceof BizException bizException) {
                throw bizException;
            }
            throw new BizException("视频任务执行失败: " + ex.getMessage());
        }
    }

    private VideoJobDto.JobResp toResp(VideoAnalysisJob job) {
        return new VideoJobDto.JobResp(job.getId(), job.getScenicId(), job.getVideoPath(), job.getAreaCode(),
            job.getDirection(), job.getSampleMs(), job.getStatus(), job.getErrorMsg(), job.getCreatedAt());
    }
}
