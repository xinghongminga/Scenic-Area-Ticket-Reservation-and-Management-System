package com.example.scencispotback.api.report;

import java.time.LocalDateTime;
import java.util.List;

// 报表 DTO
public class ReportDto {

    public record SalesTicketStat(String ticketName, Integer qty) {}

    public record SalesResp(Long scenicId,
                            LocalDateTime start,
                            LocalDateTime end,
                            Integer orderCount,
                            Integer totalAmountCent,
                            List<SalesTicketStat> byTicket) {}

    public record FlowPoint(LocalDateTime statMinute, Integer inCount, Integer outCount, Integer inParkCount) {}

    public record FlowResp(Long scenicId,
                           LocalDateTime start,
                           LocalDateTime end,
                           List<FlowPoint> points) {}

    public record HeatmapPoint(String areaCode, LocalDateTime statMinute, Integer crowdCount) {}

    public record HeatmapResp(Long scenicId,
                              LocalDateTime start,
                              LocalDateTime end,
                              List<HeatmapPoint> points) {}
}
