package com.example.scencispotback.api.aftersale;

import com.example.scencispotback.common.ApiResponse;
import com.example.scencispotback.security.Authz;
import com.example.scencispotback.service.AftersaleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class AftersaleController {

    private final AftersaleService aftersaleService;

    public AftersaleController(AftersaleService aftersaleService) {
        this.aftersaleService = aftersaleService;
    }

    @PostMapping("/api/aftersale")
    public ApiResponse<Map<String, String>> create(@Valid @RequestBody AftersaleDto.CreateReq req) {
        Authz.requireRole("TOURIST");
        String reqNo = aftersaleService.submit(req);
        return ApiResponse.ok(Map.of("reqNo", reqNo));
    }

    @GetMapping("/api/aftersale/my")
    public ApiResponse<List<AftersaleDto.ReqResp>> myList() {
        Authz.requireRole("TOURIST");
        return ApiResponse.ok(aftersaleService.myList());
    }

    @GetMapping("/api/aftersale/reschedule/options")
    public ApiResponse<List<AftersaleDto.RescheduleOptionResp>> rescheduleOptions(@RequestParam String orderNo) {
        Authz.requireRole("TOURIST");
        return ApiResponse.ok(aftersaleService.rescheduleOptions(orderNo));
    }

    @GetMapping("/api/auditor/aftersale")
    public ApiResponse<List<AftersaleDto.ReqResp>> allList(@RequestParam(required = false) String userPhone,
                                                           @RequestParam(required = false) String status) {
        Authz.requireRole("AUDITOR", "ADMIN");
        return ApiResponse.ok(aftersaleService.allList(new AftersaleDto.QueryReq(userPhone, status)));
    }

    @PutMapping("/api/auditor/aftersale/{reqNo}")
    public ApiResponse<Void> update(@PathVariable String reqNo, @RequestBody AftersaleDto.UpdateReq req) {
        Authz.requireRole("AUDITOR", "ADMIN");
        aftersaleService.updateByAuditor(reqNo, req);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/api/auditor/aftersale/{reqNo}")
    public ApiResponse<Void> delete(@PathVariable String reqNo) {
        Authz.requireRole("AUDITOR", "ADMIN");
        aftersaleService.deleteByAuditor(reqNo);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/auditor/aftersale/{reqNo}/approve")
    public ApiResponse<Void> approve(@PathVariable String reqNo, @RequestBody(required = false) AftersaleDto.AuditReq req) {
        Authz.requireRole("AUDITOR", "ADMIN");
        aftersaleService.approve(reqNo, req == null ? null : req.auditComment());
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/auditor/aftersale/{reqNo}/reject")
    public ApiResponse<Void> reject(@PathVariable String reqNo, @RequestBody(required = false) AftersaleDto.AuditReq req) {
        Authz.requireRole("AUDITOR", "ADMIN");
        aftersaleService.reject(reqNo, req == null ? null : req.auditComment());
        return ApiResponse.ok(null);
    }
}
