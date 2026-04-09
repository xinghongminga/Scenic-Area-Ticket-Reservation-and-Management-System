package com.example.scencispotback.api.order;

import com.example.scencispotback.common.ApiResponse;
import com.example.scencispotback.security.Authz;
import com.example.scencispotback.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/api/orders")
    public ApiResponse<OrderDto.CreateOrderResp> create(@Valid @RequestBody OrderDto.CreateOrderReq req) {
        Authz.requireRole("TOURIST", "ADMIN", "ANALYST", "AUDITOR");
        return ApiResponse.ok(orderService.create(req));
    }

    @PostMapping("/api/orders/{orderNo}/pay")
    public ApiResponse<OrderDto.PayResp> pay(@PathVariable String orderNo) {
        Authz.requireRole("TOURIST", "ADMIN");
        return ApiResponse.ok(orderService.pay(orderNo));
    }

    @GetMapping("/api/orders/{orderNo}")
    public ApiResponse<OrderDto.OrderDetailResp> detail(@PathVariable String orderNo) {
        Authz.requireRole("TOURIST", "ADMIN", "ANALYST", "AUDITOR");
        return ApiResponse.ok(orderService.detail(orderNo));
    }

    @GetMapping("/api/orders/{orderNo}/tickets/{verifyCode}/qr-image")
    public ResponseEntity<byte[]> ticketQrImage(@PathVariable String orderNo, @PathVariable String verifyCode) {
        Authz.requireRole("TOURIST", "ADMIN", "ANALYST", "AUDITOR");
        byte[] data = orderService.renderTicketQrImage(orderNo, verifyCode);
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(data);
    }

    @DeleteMapping("/api/orders/{orderNo}")
    public ApiResponse<Void> deleteMyOrder(@PathVariable String orderNo) {
        Authz.requireRole("TOURIST", "ADMIN", "ANALYST", "AUDITOR");
        orderService.deleteMyOrder(orderNo);
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/orders/my")
    public ApiResponse<List<OrderDto.OrderResp>> myOrders() {
        Authz.requireRole("TOURIST", "ADMIN", "ANALYST", "AUDITOR");
        return ApiResponse.ok(orderService.myOrders());
    }

    @GetMapping("/api/admin/orders")
    public ApiResponse<List<OrderDto.OrderResp>> allOrders(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String userPhone,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate visitDate,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAfter,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdBefore) {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(orderService.allOrders(
            new OrderDto.OrderQuery(status, userPhone, visitDate, createdAfter, createdBefore)));
    }

    @GetMapping("/api/admin/orders/export")
    public ResponseEntity<byte[]> exportOrders(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String userPhone,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate visitDate,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAfter,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdBefore) {
        Authz.requireRole("ADMIN");
        String csv = orderService.exportOrdersCsv(
            new OrderDto.OrderQuery(status, userPhone, visitDate, createdAfter, createdBefore));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.csv")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
