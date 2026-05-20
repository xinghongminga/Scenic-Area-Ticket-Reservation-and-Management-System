package com.example.scencispotback.api.home;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class HomeDto {

    public record HomeConsoleResp(
        OverviewStats stats,
        List<OrderItem> latestOrders,
        List<AftersaleItem> pendingAftersales
    ) {}

    public record OverviewStats(
        Integer scenicCount,
        Integer activeTicketCount,
        Integer todayOrderCount,
        Integer pendingAftersaleCount,
        Integer totalUserCount,
        Integer totalInPark
    ) {}

    public record OrderItem(
        Long id,
        String orderNo,
        String ticketName,
        String userNickname,
        String status,
        Integer totalAmountCent,
        LocalDate visitDate,
        LocalDateTime createdAt
    ) {}

    public record AftersaleItem(
        Long id,
        String reqNo,
        String reqType,
        String reason,
        String status,
        LocalDateTime createdAt
    ) {}
}
