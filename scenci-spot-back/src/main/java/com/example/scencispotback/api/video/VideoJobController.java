package com.example.scencispotback.api.video;

import com.example.scencispotback.common.ApiResponse;
import com.example.scencispotback.security.Authz;
import com.example.scencispotback.service.VideoJobService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/video-jobs")
// 视频任务控制器
public class VideoJobController {

    private final VideoJobService videoJobService;

    public VideoJobController(VideoJobService videoJobService) {
        this.videoJobService = videoJobService;
    }

    /**
     * 创建视频分析任务并返回任务ID。
     */
    @PostMapping
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody VideoJobDto.CreateReq req) {
        Authz.requireRole("ADMIN", "ANALYST");
        return ApiResponse.ok(Map.of("id", videoJobService.create(req)));
    }

    /**
     * 查询当前全部视频任务列表。
     */
    @GetMapping
    public ApiResponse<List<VideoJobDto.JobResp>> list() {
        Authz.requireRole("ADMIN", "ANALYST");
        return ApiResponse.ok(videoJobService.list());
    }

    /**
     * 触发指定任务执行并返回运行状态。
     */
    @PostMapping("/{id}/run")
    public ApiResponse<VideoJobDto.RunResp> run(@PathVariable Long id) {
        Authz.requireRole("ADMIN", "ANALYST");
        return ApiResponse.ok(videoJobService.run(id));
    }

    /**
     * 删除指定视频任务。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Authz.requireRole("ADMIN");
        videoJobService.delete(id);
        return ApiResponse.ok(null);
    }
}
