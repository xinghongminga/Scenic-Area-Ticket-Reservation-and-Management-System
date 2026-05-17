package com.example.scencispotback.api.report;

import com.example.scencispotback.common.ApiResponse;
import com.example.scencispotback.security.Authz;
import com.example.scencispotback.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/analyst/report")
// 报表控制器
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/sales")
    public ApiResponse<ReportDto.SalesResp> sales(@RequestParam Long scenicId,
                                                  @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
                                                  @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        Authz.requireRole("ANALYST", "ADMIN");
        return ApiResponse.ok(reportService.sales(scenicId, start, end));
    }

    @GetMapping("/flow")
    public ApiResponse<ReportDto.FlowResp> flow(@RequestParam Long scenicId,
                                                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
                                                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        Authz.requireRole("ANALYST", "ADMIN");
        return ApiResponse.ok(reportService.flow(scenicId, start, end));
    }

    @GetMapping("/heatmap")
    public ApiResponse<ReportDto.HeatmapResp> heatmap(@RequestParam Long scenicId,
                                                      @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
                                                      @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        Authz.requireRole("ANALYST", "ADMIN");
        return ApiResponse.ok(reportService.heatmap(scenicId, start, end));
    }

    @GetMapping("/sales/export")
    public ResponseEntity<byte[]> exportSales(@RequestParam Long scenicId,
                                              @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
                                              @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        Authz.requireRole("ANALYST", "ADMIN");
        String csv = reportService.exportSalesCsv(scenicId, start, end);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales_report.csv")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/flow/export")
    public ResponseEntity<byte[]> exportFlow(@RequestParam Long scenicId,
                                             @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
                                             @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        Authz.requireRole("ANALYST", "ADMIN");
        String csv = reportService.exportFlowCsv(scenicId, start, end);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=flow_report.csv")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
