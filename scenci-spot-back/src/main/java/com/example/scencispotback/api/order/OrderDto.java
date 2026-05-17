package com.example.scencispotback.api.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// 订单 DTO
public class OrderDto {

    public record CreateOrderReq(@NotNull Long ticketId,
                                 @NotNull LocalDate visitDate,
                                 @NotNull Long timeslotId,
                                 @NotNull @Min(1) Integer qty) {}

    public record CreateOrderResp(String orderNo, Integer totalAmountCent, String status) {}

    public record PayResp(String orderNo,
                          String payNo,
                          String gatewayTradeNo,
                          Integer amountCent,
                          List<String> qrCodes,
                          Long notificationId,
                          String notificationTitle,
                          String notificationContent) {}

    public record OrderResp(String orderNo,
                            Long userId,
                            String userNickname,
                            String userPhone,
                            String userFullName,
                            String userIdCardNo,
                            Long scenicId,
                            LocalDate visitDate,
                            Long timeslotId,
                            Integer totalAmountCent,
                            String status,
                            LocalDateTime createdAt,
                            String ticketName,
                            String ticketImageUrl) {}

    public record OrderQuery(String status,
                             String userPhone,
                             LocalDate visitDate,
                             LocalDateTime createdAfter, LocalDateTime createdBefore) {
        // Backwards-compatible constructor for existing 2-arg calls
        public OrderQuery(String status) {
            this(status, null, null, null, null);
        }
    }

    public record OrderItemResp(Long ticketId,
                                String ticketName,
                                Integer unitPriceCent,
                                Integer qty,
                                Integer amountCent) {}

    public record OrderTicketResp(String qrCode,
                                  String verifyCode,
                                  String status) {}

    public record OrderDetailResp(OrderResp order,
                                  List<OrderItemResp> items,
                                  List<OrderTicketResp> tickets) {}
}
