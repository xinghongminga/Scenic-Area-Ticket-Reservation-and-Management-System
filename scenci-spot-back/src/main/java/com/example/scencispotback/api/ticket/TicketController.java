package com.example.scencispotback.api.ticket;

import com.example.scencispotback.common.ApiResponse;
import com.example.scencispotback.security.Authz;
import com.example.scencispotback.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
// 门票控制器
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * 用户侧门票列表查询：按景区、票种、价格区间和关键词过滤，仅返回可售门票。
     */
    @GetMapping("/api/tickets")
    public ApiResponse<List<TicketDto.TicketListResp>> list(@RequestParam(required = false) Long scenicId,
                                                            @RequestParam(required = false) String ticketType,
                                                            @RequestParam(required = false) Integer priceMin,
                                                            @RequestParam(required = false) Integer priceMax,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return ApiResponse.ok(ticketService.list(new TicketDto.TicketQuery(scenicId, ticketType, priceMin, priceMax, keyword, date), true));
    }

    /**
     * 管理端门票列表查询：支持查看全部状态门票（含下架），需管理员权限。
     */
    @GetMapping("/api/admin/tickets")
    public ApiResponse<List<TicketDto.TicketListResp>> adminList(@RequestParam(required = false) Long scenicId,
                                                                 @RequestParam(required = false) String ticketType,
                                                                 @RequestParam(required = false) Integer priceMin,
                                                                 @RequestParam(required = false) Integer priceMax,
                                                                 @RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(ticketService.list(new TicketDto.TicketQuery(scenicId, ticketType, priceMin, priceMax, keyword, date), false));
    }

    /**
     * 门票详情查询：可携带日期用于返回该日期的可用库存/场次信息。
     */
    @GetMapping("/api/tickets/{id}")
    public ApiResponse<TicketDto.TicketDetailResp> detail(@PathVariable Long id,
                                                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return ApiResponse.ok(ticketService.detail(id, date));
    }

    /**
     * 查询指定门票在某一天的分时段库存（如上午场/下午场）。
     */
    @GetMapping("/api/tickets/{id}/inventory")
    public ApiResponse<List<TicketDto.InventoryResp>> inventory(@PathVariable Long id,
                                                                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return ApiResponse.ok(ticketService.inventory(id, date));
    }

    /**
     * 管理员新增门票，返回新建门票 ID。
     */
    @PostMapping("/api/admin/tickets")
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody TicketDto.AdminTicketUpsertReq req) {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(Map.of("id", ticketService.create(req)));
    }

    /**
     * 管理员编辑门票基础信息（名称、价格、图片、有效期、场次配置等）。
     */
    @PutMapping("/api/admin/tickets/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody TicketDto.AdminTicketUpsertReq req) {
        Authz.requireRole("ADMIN");
        ticketService.update(id, req);
        return ApiResponse.ok(null);
    }

    /**
     * 管理员上下架门票。
     */
    @PutMapping("/api/admin/tickets/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody TicketDto.TicketStatusReq req) {
        Authz.requireRole("ADMIN");
        ticketService.updateStatus(id, req.status());
        return ApiResponse.ok(null);
    }

    /**
     * 管理员删除门票。
     */
    @DeleteMapping("/api/admin/tickets/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Authz.requireRole("ADMIN");
        ticketService.delete(id);
        return ApiResponse.ok(null);
    }

    /**
     * 管理员手动调整指定日期/场次库存。
     */
    @PostMapping("/api/admin/inventory/adjust")
    public ApiResponse<Void> adjustInventory(@Valid @RequestBody TicketDto.AdjustInventoryReq req) {
        Authz.requireRole("ADMIN");
        ticketService.adjustInventory(req);
        return ApiResponse.ok(null);
    }

    /**
     * 导出门票 Excel（可按景区过滤），用于后台下载。
     */
    @GetMapping("/api/admin/tickets/export")
    public ResponseEntity<byte[]> exportExcel(@RequestParam(required = false) Long scenicId) {
        Authz.requireRole("ADMIN");
        byte[] data = ticketService.exportExcel(scenicId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tickets.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(data);
    }

    /**
     * 下载门票导入模板 Excel。
     */
    @GetMapping("/api/admin/tickets/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        Authz.requireRole("ADMIN");
        byte[] data = ticketService.exportImportTemplate();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ticket_template.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(data);
    }

    /**
     * 批量导入门票 Excel，返回成功导入条数。
     */
    @PostMapping("/api/admin/tickets/import")
    public ApiResponse<Map<String, Integer>> importExcel(@RequestParam("file") MultipartFile file) {
        Authz.requireRole("ADMIN");
        int count = ticketService.importExcel(file);
        return ApiResponse.ok(Map.of("imported", count));
    }
}
