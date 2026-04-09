package com.example.scencispotback.api.scenic;

import com.example.scencispotback.common.ApiResponse;
import com.example.scencispotback.security.Authz;
import com.example.scencispotback.service.ScenicService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ScenicController {

    private final ScenicService scenicService;

    public ScenicController(ScenicService scenicService) {
        this.scenicService = scenicService;
    }

    // ===== Scenic Area =====

    @GetMapping("/api/scenic")
    public ApiResponse<List<ScenicDto.ScenicResp>> listScenics() {
        return ApiResponse.ok(scenicService.listScenics(false));
    }

    @GetMapping("/api/admin/scenic")
    public ApiResponse<List<ScenicDto.ScenicResp>> adminListScenics() {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(scenicService.listScenics(true));
    }

    @GetMapping("/api/admin/scenic/{id}")
    public ApiResponse<ScenicDto.ScenicResp> getScenic(@PathVariable Long id) {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(scenicService.getScenic(id));
    }

    @PostMapping("/api/admin/scenic")
    public ApiResponse<Map<String, Long>> createScenic(@Valid @RequestBody ScenicDto.ScenicUpsertReq req) {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(Map.of("id", scenicService.createScenic(req)));
    }

    @PutMapping("/api/admin/scenic/{id}")
    public ApiResponse<Void> updateScenic(@PathVariable Long id, @Valid @RequestBody ScenicDto.ScenicUpsertReq req) {
        Authz.requireRole("ADMIN");
        scenicService.updateScenic(id, req);
        return ApiResponse.ok(null);
    }

    @PutMapping("/api/admin/scenic/{id}/status")
    public ApiResponse<Void> updateScenicStatus(@PathVariable Long id, @Valid @RequestBody ScenicDto.ScenicStatusReq req) {
        Authz.requireRole("ADMIN");
        scenicService.updateScenicStatus(id, req.status());
        return ApiResponse.ok(null);
    }

    // ===== Timeslot =====

    @GetMapping("/api/timeslots")
    public ApiResponse<List<ScenicDto.TimeslotResp>> listTimeslots(@RequestParam Long scenicId) {
        return ApiResponse.ok(scenicService.listTimeslots(scenicId));
    }

    @PostMapping("/api/admin/timeslots")
    public ApiResponse<Map<String, Long>> createTimeslot(@Valid @RequestBody ScenicDto.TimeslotUpsertReq req) {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(Map.of("id", scenicService.createTimeslot(req)));
    }

    @PutMapping("/api/admin/timeslots/{id}")
    public ApiResponse<Void> updateTimeslot(@PathVariable Long id, @Valid @RequestBody ScenicDto.TimeslotUpsertReq req) {
        Authz.requireRole("ADMIN");
        scenicService.updateTimeslot(id, req);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/api/admin/timeslots/{id}")
    public ApiResponse<Void> deleteTimeslot(@PathVariable Long id) {
        Authz.requireRole("ADMIN");
        scenicService.deleteTimeslot(id);
        return ApiResponse.ok(null);
    }

    // ===== RefundRule =====

    @GetMapping("/api/refund-rules")
    public ApiResponse<List<ScenicDto.RefundRuleResp>> listRefundRules(@RequestParam Long scenicId) {
        return ApiResponse.ok(scenicService.listRefundRules(scenicId));
    }

    @PostMapping("/api/admin/refund-rules")
    public ApiResponse<Map<String, Long>> createRefundRule(@Valid @RequestBody ScenicDto.RefundRuleUpsertReq req) {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(Map.of("id", scenicService.createRefundRule(req)));
    }

    @PutMapping("/api/admin/refund-rules/{id}")
    public ApiResponse<Void> updateRefundRule(@PathVariable Long id, @Valid @RequestBody ScenicDto.RefundRuleUpsertReq req) {
        Authz.requireRole("ADMIN");
        scenicService.updateRefundRule(id, req);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/api/admin/refund-rules/{id}")
    public ApiResponse<Void> deleteRefundRule(@PathVariable Long id) {
        Authz.requireRole("ADMIN");
        scenicService.deleteRefundRule(id);
        return ApiResponse.ok(null);
    }
}
