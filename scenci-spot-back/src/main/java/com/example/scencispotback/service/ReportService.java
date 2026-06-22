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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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

    /**
     * 查询销售统计数据。
     */
    public ReportDto.SalesResp sales(Long scenicId, LocalDateTime start, LocalDateTime end) {
        Integer totalAmount = reportMapper.salesAmount(scenicId, start, end);
        Integer orderCount = reportMapper.paidOrderCount(scenicId, start, end);
        List<ReportDto.SalesTicketStat> byTicket = reportMapper.salesByTicket(scenicId, start, end).stream()
            .map(this::mapSalesTicket)
            .toList();
        return new ReportDto.SalesResp(scenicId, start, end, orderCount, totalAmount, byTicket);
    }

    /**
     * 查询客流趋势数据。
     */
    public ReportDto.FlowResp flow(Long scenicId, LocalDateTime start, LocalDateTime end) {
        List<ReportDto.FlowPoint> points = flowMinuteMapper.listRange(scenicId, start, end).stream()
            .map(this::mapFlow)
            .toList();
        return new ReportDto.FlowResp(scenicId, start, end, points);
    }

    /**
     * 查询热力图数据。
     */
    public ReportDto.HeatmapResp heatmap(Long scenicId, LocalDateTime start, LocalDateTime end) {
        List<ReportDto.HeatmapPoint> points = flowAreaMinuteMapper.listRange(scenicId, start, end).stream()
            .map(p -> new ReportDto.HeatmapPoint(p.getAreaCode(), p.getStatMinute(), p.getCrowdCount()))
            .toList();
        return new ReportDto.HeatmapResp(scenicId, start, end, points);
    }

    /**
     * 生成销售报表Excel内容。
     */
    public byte[] exportSalesExcel(Long scenicId, LocalDateTime start, LocalDateTime end) {
        ReportDto.SalesResp sales = sales(scenicId, start, end);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("销量报表");
            
            // 表头
            Row header1 = sheet.createRow(0);
            header1.createCell(0).setCellValue("景区ID");
            header1.createCell(1).setCellValue("开始时间");
            header1.createCell(2).setCellValue("结束时间");
            header1.createCell(3).setCellValue("总计订单数");
            header1.createCell(4).setCellValue("总计金额(元)");
            
            // 总计数据
            Row row1 = sheet.createRow(1);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            row1.createCell(0).setCellValue(sales.scenicId() == null ? "" : sales.scenicId().toString());
            row1.createCell(1).setCellValue(sales.start() == null ? "" : sales.start().format(fmt));
            row1.createCell(2).setCellValue(sales.end() == null ? "" : sales.end().format(fmt));
            row1.createCell(3).setCellValue(sales.orderCount() == null ? 0 : sales.orderCount());
            row1.createCell(4).setCellValue(sales.totalAmountCent() == null ? 0 : sales.totalAmountCent() / 100.0);

            // 门票明细表头
            Row header2 = sheet.createRow(3);
            header2.createCell(0).setCellValue("门票名称");
            header2.createCell(1).setCellValue("销售数量(张)");
            header2.createCell(2).setCellValue("销售金额(元)");

            int rowNum = 4;
            for (ReportDto.SalesTicketStat s : sales.byTicket()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(s.ticketName());
                row.createCell(1).setCellValue(s.qty() == null ? 0 : s.qty());
                row.createCell(2).setCellValue(s.amountCent() == null ? 0 : s.amountCent() / 100.0);
            }
            
            for (int i = 0; i < 5; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }
            
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    /**
     * 生成客流报表Excel内容。
     */
    public byte[] exportFlowExcel(Long scenicId, LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("客流报表");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("统计时间");
            header.createCell(1).setCellValue("入园人数");
            header.createCell(2).setCellValue("出园人数");
            header.createCell(3).setCellValue("园内总人数");

            int rowNum = 1;
            for (FlowMinutePoint p : flowMinuteMapper.listRange(scenicId, start, end)) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getStatMinute() == null ? "" : p.getStatMinute().format(fmt));
                row.createCell(1).setCellValue(p.getInCount() == null ? 0 : p.getInCount());
                row.createCell(2).setCellValue(p.getOutCount() == null ? 0 : p.getOutCount());
                row.createCell(3).setCellValue(p.getInParkCount() == null ? 0 : p.getInParkCount());
            }
            
            for (int i = 0; i < 4; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }
            
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    /**
     * 转换销售统计行数据。
     */
    private ReportDto.SalesTicketStat mapSalesTicket(Map<String, Object> row) {
        String ticketName = String.valueOf(row.getOrDefault("ticketName", "未命名门票"));
        Integer qty = Integer.parseInt(String.valueOf(row.getOrDefault("qty", 0)));
        Integer amountCent = Integer.parseInt(String.valueOf(row.getOrDefault("amountCent", 0)));
        return new ReportDto.SalesTicketStat(ticketName, qty, amountCent);
    }

    /**
     * 转换客流点位数据。
     */
    private ReportDto.FlowPoint mapFlow(FlowMinutePoint p) {
        return new ReportDto.FlowPoint(p.getStatMinute(), p.getInCount(), p.getOutCount(), p.getInParkCount());
    }
}
