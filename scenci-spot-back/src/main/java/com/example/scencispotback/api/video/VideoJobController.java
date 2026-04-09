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
public class VideoJobController {

    private final VideoJobService videoJobService;

    public VideoJobController(VideoJobService videoJobService) {
        this.videoJobService = videoJobService;
    }

    @PostMapping
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody VideoJobDto.CreateReq req) {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(Map.of("id", videoJobService.create(req)));
    }

    @GetMapping
    public ApiResponse<List<VideoJobDto.JobResp>> list() {
        Authz.requireRole("ADMIN", "ANALYST");
        return ApiResponse.ok(videoJobService.list());
    }

    @PostMapping("/{id}/run")
    public ApiResponse<VideoJobDto.RunResp> run(@PathVariable Long id) {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(videoJobService.run(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Authz.requireRole("ADMIN");
        videoJobService.delete(id);
        return ApiResponse.ok(null);
    }
}
