package com.example.scencispotback.api.flow;

import com.example.scencispotback.common.ApiResponse;
import com.example.scencispotback.security.Authz;
import com.example.scencispotback.service.FlowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
// 客流控制器
public class FlowController {

    private final FlowService flowService;

    public FlowController(FlowService flowService) {
        this.flowService = flowService;
    }

    @PostMapping("/api/admin/flow-threshold")
    public ApiResponse<Map<String, Long>> createThreshold(@Valid @RequestBody FlowDto.ThresholdUpsertReq req) {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(Map.of("id", flowService.createThreshold(req)));
    }

    @PostMapping("/api/admin/flow-thresholds")
    public ApiResponse<Map<String, Long>> createThresholds(@Valid @RequestBody FlowDto.ThresholdUpsertReq req) {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(Map.of("id", flowService.createThreshold(req)));
    }

    @PutMapping("/api/admin/flow-threshold/{id}")
    public ApiResponse<Void> updateThreshold(@PathVariable Long id, @Valid @RequestBody FlowDto.ThresholdUpsertReq req) {
        Authz.requireRole("ADMIN");
        flowService.updateThreshold(id, req);
        return ApiResponse.ok(null);
    }

    @PutMapping("/api/admin/flow-thresholds/{id}")
    public ApiResponse<Void> updateThresholds(@PathVariable Long id, @Valid @RequestBody FlowDto.ThresholdUpsertReq req) {
        Authz.requireRole("ADMIN");
        flowService.updateThreshold(id, req);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/api/admin/flow-thresholds/{id}")
    public ApiResponse<Void> deleteThreshold(@PathVariable Long id) {
        Authz.requireRole("ADMIN");
        flowService.deleteThreshold(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/admin/flow-threshold")
    public ApiResponse<List<FlowDto.ThresholdResp>> listThreshold(@RequestParam Long scenicId) {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(flowService.listThreshold(scenicId));
    }

    @GetMapping("/api/admin/flow-thresholds")
    public ApiResponse<List<FlowDto.ThresholdResp>> listThresholds(@RequestParam(required = false) Long scenicId) {
        Authz.requireRole("ADMIN");
        if (scenicId != null) {
            return ApiResponse.ok(flowService.listThreshold(scenicId));
        }
        return ApiResponse.ok(List.of());
    }

    @GetMapping("/api/admin/flow/dashboard")
    public ApiResponse<FlowDto.DashboardResp> dashboard(@RequestParam Long scenicId,
                                                        @RequestParam(required = false) Integer minutes) {
        Authz.requireRole("ADMIN", "ANALYST");
        return ApiResponse.ok(flowService.dashboard(scenicId, minutes));
    }
}
