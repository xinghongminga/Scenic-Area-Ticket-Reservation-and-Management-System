package com.example.scencispotback.service;

import com.example.scencispotback.api.report.ReportDto;
import com.example.scencispotback.domain.FlowAreaMinutePoint;
import com.example.scencispotback.domain.FlowMinutePoint;
import com.example.scencispotback.mapper.FlowAreaMinuteMapper;
import com.example.scencispotback.mapper.FlowMinuteMapper;
import com.example.scencispotback.mapper.ReportMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
// 报表服务
public class ReportService {

    private final ReportMapper reportMapper;
    private final FlowMinuteMapper flowMinuteMapper;
    private final FlowAreaMinuteMapper flowAreaMinuteMapper;

    public ReportService(ReportMapper reportMapper,
                         FlowMinuteMapper flowMinuteMapper,
                         FlowAreaMinuteMapper flowAreaMinuteMapper) {
        this.reportMapper = reportMapper;
        this.flowMinuteMapper = flowMinuteMapper;
        this.flowAreaMinuteMapper = flowAreaMinuteMapper;
    }

    public ReportDto.SalesResp sales(Long scenicId, LocalDateTime start, LocalDateTime end) {
        Integer totalAmount = reportMapper.salesAmount(scenicId, start, end);
        Integer orderCount = reportMapper.paidOrderCount(scenicId, start, end);
        List<ReportDto.SalesTicketStat> byTicket = reportMapper.salesByTicket(scenicId, start, end).stream()
            .map(this::mapSalesTicket)
            .toList();
        return new ReportDto.SalesResp(scenicId, start, end, orderCount, totalAmount, byTicket);
    }

    public ReportDto.FlowResp flow(Long scenicId, LocalDateTime start, LocalDateTime end) {
        List<ReportDto.FlowPoint> points = flowMinuteMapper.listRange(scenicId, start, end).stream()
            .map(this::mapFlow)
            .toList();
        return new ReportDto.FlowResp(scenicId, start, end, points);
    }

    public ReportDto.HeatmapResp heatmap(Long scenicId, LocalDateTime start, LocalDateTime end) {
        List<ReportDto.HeatmapPoint> points = flowAreaMinuteMapper.listRange(scenicId, start, end).stream()
            .map(p -> new ReportDto.HeatmapPoint(p.getAreaCode(), p.getStatMinute(), p.getCrowdCount()))
            .toList();
        return new ReportDto.HeatmapResp(scenicId, start, end, points);
    }

    public String exportSalesCsv(Long scenicId, LocalDateTime start, LocalDateTime end) {
        ReportDto.SalesResp sales = sales(scenicId, start, end);
        StringBuilder sb = new StringBuilder();
        sb.append("scenicId,start,end,orderCount,totalAmountCent\n");
        sb.append(sales.scenicId()).append(',').append(sales.start()).append(',').append(sales.end()).append(',')
            .append(sales.orderCount()).append(',').append(sales.totalAmountCent()).append('\n');
        sb.append("ticketName,qty\n");
        for (ReportDto.SalesTicketStat s : sales.byTicket()) {
            sb.append(s.ticketName()).append(',').append(s.qty()).append('\n');
        }
        return sb.toString();
    }

    public String exportFlowCsv(Long scenicId, LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        sb.append("statMinute,inCount,outCount,inParkCount\n");
        for (FlowMinutePoint p : flowMinuteMapper.listRange(scenicId, start, end)) {
            sb.append(p.getStatMinute().format(fmt)).append(',')
                .append(p.getInCount()).append(',')
                .append(p.getOutCount()).append(',')
                .append(p.getInParkCount()).append('\n');
        }
        return sb.toString();
    }

    private ReportDto.SalesTicketStat mapSalesTicket(Map<String, Object> row) {
        String ticketName = String.valueOf(row.getOrDefault("ticketName", "未命名门票"));
        Integer qty = Integer.parseInt(String.valueOf(row.getOrDefault("qty", 0)));
        return new ReportDto.SalesTicketStat(ticketName, qty);
    }

    private ReportDto.FlowPoint mapFlow(FlowMinutePoint p) {
        return new ReportDto.FlowPoint(p.getStatMinute(), p.getInCount(), p.getOutCount(), p.getInParkCount());
    }
}
