package com.example.scencispotback.api.scenic;

import com.example.scencispotback.common.ApiResponse;
import com.example.scencispotback.security.Authz;
import com.example.scencispotback.service.ScenicService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
// 景区控制器
public class ScenicController {

    private final ScenicService scenicService;

    public ScenicController(ScenicService scenicService) {
        this.scenicService = scenicService;
    }

    // ===== Scenic Area =====

    @GetMapping("/api/scenic")
    /**
     * 获取前台可见景区列表。
     */
    public ApiResponse<List<ScenicDto.ScenicResp>> listScenics() {
        return ApiResponse.ok(scenicService.listScenics(false));
    }

    @GetMapping("/api/admin/scenic")
    /**
     * 管理员获取全部景区列表。
     */
    public ApiResponse<List<ScenicDto.ScenicResp>> adminListScenics() {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(scenicService.listScenics(true));
    }

    @GetMapping("/api/admin/scenic/{id}")
    /**
     * 管理员获取单个景区详情。
     */
    public ApiResponse<ScenicDto.ScenicResp> getScenic(@PathVariable Long id) {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(scenicService.getScenic(id));
    }

    @PostMapping("/api/admin/scenic")
    /**
     * 管理员创建景区。
     */
    public ApiResponse<Map<String, Long>> createScenic(@Valid @RequestBody ScenicDto.ScenicUpsertReq req) {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(Map.of("id", scenicService.createScenic(req)));
    }

    @PutMapping("/api/admin/scenic/{id}")
    /**
     * 管理员更新景区信息。
     */
    public ApiResponse<Void> updateScenic(@PathVariable Long id, @Valid @RequestBody ScenicDto.ScenicUpsertReq req) {
        Authz.requireRole("ADMIN");
        scenicService.updateScenic(id, req);
        return ApiResponse.ok(null);
    }

    @PutMapping("/api/admin/scenic/{id}/status")
    /**
     * 管理员更新景区状态（上/下架）。
     */
    public ApiResponse<Void> updateScenicStatus(@PathVariable Long id, @Valid @RequestBody ScenicDto.ScenicStatusReq req) {
        Authz.requireRole("ADMIN");
        scenicService.updateScenicStatus(id, req.status());
        return ApiResponse.ok(null);
    }

    // ===== Timeslot =====

    @GetMapping("/api/timeslots")
    /**
     * 查询指定景区的时段列表。
     */
    public ApiResponse<List<ScenicDto.TimeslotResp>> listTimeslots(@RequestParam Long scenicId) {
        return ApiResponse.ok(scenicService.listTimeslots(scenicId));
    }

    @PostMapping("/api/admin/timeslots")
    /**
     * 管理员创建景区时段。
     */
    public ApiResponse<Map<String, Long>> createTimeslot(@Valid @RequestBody ScenicDto.TimeslotUpsertReq req) {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(Map.of("id", scenicService.createTimeslot(req)));
    }

    @PutMapping("/api/admin/timeslots/{id}")
    /**
     * 管理员更新景区时段。
     */
    public ApiResponse<Void> updateTimeslot(@PathVariable Long id, @Valid @RequestBody ScenicDto.TimeslotUpsertReq req) {
        Authz.requireRole("ADMIN");
        scenicService.updateTimeslot(id, req);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/api/admin/timeslots/{id}")
    /**
     * 管理员删除景区时段。
     */
    public ApiResponse<Void> deleteTimeslot(@PathVariable Long id) {
        Authz.requireRole("ADMIN");
        scenicService.deleteTimeslot(id);
        return ApiResponse.ok(null);
    }

    // ===== RefundRule =====

    @GetMapping("/api/refund-rules")
    /**
     * 查询指定景区的退改规则列表。
     */
    public ApiResponse<List<ScenicDto.RefundRuleResp>> listRefundRules(@RequestParam Long scenicId) {
        return ApiResponse.ok(scenicService.listRefundRules(scenicId));
    }

    @PostMapping("/api/admin/refund-rules")
    /**
     * 管理员创建退改规则。
     */
    public ApiResponse<Map<String, Long>> createRefundRule(@Valid @RequestBody ScenicDto.RefundRuleUpsertReq req) {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(Map.of("id", scenicService.createRefundRule(req)));
    }

    @PutMapping("/api/admin/refund-rules/{id}")
    /**
     * 管理员更新退改规则。
     */
    public ApiResponse<Void> updateRefundRule(@PathVariable Long id, @Valid @RequestBody ScenicDto.RefundRuleUpsertReq req) {
        Authz.requireRole("ADMIN");
        scenicService.updateRefundRule(id, req);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/api/admin/refund-rules/{id}")
    /**
     * 管理员删除退改规则。
     */
    public ApiResponse<Void> deleteRefundRule(@PathVariable Long id) {
        Authz.requireRole("ADMIN");
        scenicService.deleteRefundRule(id);
        return ApiResponse.ok(null);
    }
}
