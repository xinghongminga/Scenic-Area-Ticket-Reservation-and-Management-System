package com.example.scencispotback.service;

import com.example.scencispotback.api.ticket.TicketDto;
import com.example.scencispotback.common.BizException;
import com.example.scencispotback.domain.ScenicArea;
import com.example.scencispotback.domain.Ticket;
import com.example.scencispotback.domain.TicketInventoryRow;
import com.example.scencispotback.domain.TicketProject;
import com.example.scencispotback.mapper.ScenicAreaMapper;
import com.example.scencispotback.mapper.TicketInventoryMapper;
import com.example.scencispotback.mapper.TicketMapper;
import com.example.scencispotback.mapper.TicketProjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private final TicketMapper ticketMapper;
    private final TicketInventoryMapper ticketInventoryMapper;
    private final TicketProjectMapper ticketProjectMapper;
    private final ScenicAreaMapper scenicAreaMapper;

    public TicketService(TicketMapper ticketMapper,
                         TicketInventoryMapper ticketInventoryMapper,
                         TicketProjectMapper ticketProjectMapper,
                         ScenicAreaMapper scenicAreaMapper) {
        this.ticketMapper = ticketMapper;
        this.ticketInventoryMapper = ticketInventoryMapper;
        this.ticketProjectMapper = ticketProjectMapper;
        this.scenicAreaMapper = scenicAreaMapper;
    }

    public List<TicketDto.TicketListResp> list(TicketDto.TicketQuery query, boolean onlyOnline) {
        List<Ticket> tickets = ticketMapper.list(query.scenicId(), query.ticketType(), query.priceMin(), query.priceMax(), query.keyword(), onlyOnline);
        if (tickets.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Long>> projectIdsByTicket = loadProjectIdsByTicket(tickets);
        Map<Long, String> projectNamesById = loadProjectNameById(projectIdsByTicket.values().stream().flatMap(List::stream).distinct().toList());
        return tickets.stream().map(t -> toListResp(t, projectIdsByTicket.getOrDefault(t.getId(), List.of()), projectNamesById)).toList();
    }

    public TicketDto.TicketDetailResp detail(Long id, LocalDate date) {
        Ticket ticket = ticketMapper.findById(id);
        if (ticket == null) {
            throw new BizException("门票不存在");
        }
        List<Long> projectIds = ticketProjectMapper.listByTicketId(id).stream().map(TicketProject::getProjectId).distinct().toList();
        Map<Long, String> projectNamesById = loadProjectNameById(projectIds);
        List<TicketDto.InventoryResp> inv = inventory(id, date == null ? LocalDate.now() : date);
        return new TicketDto.TicketDetailResp(toListResp(ticket, projectIds, projectNamesById), inv);
    }

    public List<TicketDto.InventoryResp> inventory(Long ticketId, LocalDate date) {
        Ticket ticket = ticketMapper.findById(ticketId);
        if (ticket == null) {
            throw new BizException("门票不存在");
        }
        LocalDate queryDate = date == null ? LocalDate.now() : date;
        if (!isInValidDateRange(ticket, queryDate)) {
            return List.of();
        }
        boolean morningEnabled = (ticket.getMorningEnabled() == null ? 1 : ticket.getMorningEnabled()) == 1;
        boolean afternoonEnabled = (ticket.getAfternoonEnabled() == null ? 1 : ticket.getAfternoonEnabled()) == 1;
        List<TicketInventoryRow> rows = ticketInventoryMapper.listByTicketAndDate(ticketId, queryDate);
        Map<Long, TicketInventoryRow> byTimeslot = rows.stream()
            .collect(Collectors.toMap(TicketInventoryRow::getTimeslotId, r -> r, (a, b) -> a));

        List<TicketDto.InventoryResp> result = new ArrayList<>();
        if (morningEnabled && byTimeslot.containsKey(1L)) {
            result.add(toInventoryResp(byTimeslot.get(1L)));
        }
        if (afternoonEnabled && byTimeslot.containsKey(2L)) {
            result.add(toInventoryResp(byTimeslot.get(2L)));
        }
        return result;
    }

    @Transactional
    public Long create(TicketDto.AdminTicketUpsertReq req) {
        validateProjectIds(req.projectIds());
        Ticket t = new Ticket();
        t.setScenicId(req.scenicId());
        t.setName(req.name());
        t.setImageUrl(req.imageUrl());
        t.setDescription(req.description());
        t.setTicketType(req.ticketType());
        t.setPriceCent(req.priceCent());
        t.setStockQty(req.stockQty());
        t.setMorningEnabled(req.morningEnabled() == null ? 1 : req.morningEnabled());
        t.setAfternoonEnabled(req.afternoonEnabled() == null ? 1 : req.afternoonEnabled());
        ensureAtLeastOneTimeslotEnabled(t.getMorningEnabled(), t.getAfternoonEnabled());
        t.setValidFrom(req.validFrom());
        t.setValidTo(req.validTo());
        t.setRefundRuleId(req.refundRuleId());
        t.setStatus(1);
        ticketMapper.insert(t);
        bindTicketProjects(t.getId(), req.projectIds());
        upsertInventoryByTimeslot(t.getId(), req.stockQty(), req.morningStockQty(), req.afternoonStockQty(), t.getMorningEnabled(), t.getAfternoonEnabled());
        return t.getId();
    }

    @Transactional
    public void update(Long id, TicketDto.AdminTicketUpsertReq req) {
        validateProjectIds(req.projectIds());
        Ticket old = ticketMapper.findById(id);
        if (old == null) {
            throw new BizException("门票不存在");
        }
        old.setName(req.name());
        old.setImageUrl(req.imageUrl());
        old.setDescription(req.description());
        old.setTicketType(req.ticketType());
        old.setPriceCent(req.priceCent());
        old.setStockQty(req.stockQty());
        old.setMorningEnabled(req.morningEnabled() == null ? 1 : req.morningEnabled());
        old.setAfternoonEnabled(req.afternoonEnabled() == null ? 1 : req.afternoonEnabled());
        ensureAtLeastOneTimeslotEnabled(old.getMorningEnabled(), old.getAfternoonEnabled());
        old.setValidFrom(req.validFrom());
        old.setValidTo(req.validTo());
        old.setRefundRuleId(req.refundRuleId());
        ticketMapper.update(old);
        ticketProjectMapper.deleteByTicketId(id);
        bindTicketProjects(id, req.projectIds());
        upsertInventoryByTimeslot(old.getId(), req.stockQty(), req.morningStockQty(), req.afternoonStockQty(), old.getMorningEnabled(), old.getAfternoonEnabled());
    }

    public void updateStatus(Long id, Integer status) {
        if (ticketMapper.updateStatus(id, status) == 0) {
            throw new BizException("门票不存在");
        }
    }

    @Transactional
    public void delete(Long id) {
        if (ticketMapper.findById(id) == null) {
            throw new BizException("门票不存在");
        }
        ticketInventoryMapper.deleteByTicketId(id);
        ticketProjectMapper.deleteByTicketId(id);
        ticketMapper.deleteById(id);
    }

    @Transactional
    public void adjustInventory(TicketDto.AdjustInventoryReq req) {
        TicketInventoryRow row = ticketInventoryMapper.lockOne(req.ticketId(), req.visitDate(), req.timeslotId());
        if (row == null) {
            if (req.delta() < 0) {
                throw new BizException("库存不存在，无法减少");
            }
            ticketInventoryMapper.create(req.ticketId(), req.visitDate(), req.timeslotId(), req.delta());
            return;
        }
        ticketInventoryMapper.adjustTotal(row.getId(), req.delta());
    }

    // ===== Excel Export =====

    public byte[] exportExcel(Long scenicId) {
        List<Ticket> tickets = ticketMapper.list(scenicId, null, null, null, null, false);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("门票列表");
            String[] headers = {"ID", "景区ID", "门票名称", "描述/入园须知", "门票类型", "价格(分)", "库存", "有效期开始", "有效期结束", "退改规则ID", "状态"};
            Row headRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headRow.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (Ticket t : tickets) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(t.getId());
                row.createCell(1).setCellValue(t.getScenicId());
                row.createCell(2).setCellValue(t.getName() != null ? t.getName() : "");
                row.createCell(3).setCellValue(t.getDescription() != null ? t.getDescription() : "");
                row.createCell(4).setCellValue(toTicketTypeZh(t.getTicketType()));
                row.createCell(5).setCellValue(t.getPriceCent() != null ? t.getPriceCent() : 0);
                row.createCell(6).setCellValue(t.getStockQty() != null ? t.getStockQty() : 0);
                row.createCell(7).setCellValue(t.getValidFrom() != null ? t.getValidFrom().toString() : "");
                row.createCell(8).setCellValue(t.getValidTo() != null ? t.getValidTo().toString() : "");
                row.createCell(9).setCellValue(t.getRefundRuleId() != null ? t.getRefundRuleId() : 0);
                row.createCell(10).setCellValue(t.getStatus() == 1 ? "上架" : "下架");
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BizException("导出Excel失败: " + e.getMessage());
        }
    }

    public byte[] exportImportTemplate() {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("门票导入模板");
            String[] headers = {"门票名称", "景区项目", "票种", "价格(元)", "上午库存", "下午库存", "场次", "状态", "有效期开始", "有效期结束", "退款规则ID"};
            Row headRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headRow.createCell(i).setCellValue(headers[i]);
                sheet.setColumnWidth(i, 18 * 256);
            }

            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("成人日场票");
            exampleRow.createCell(1).setCellValue("1");
            exampleRow.createCell(2).setCellValue("单人票");
            exampleRow.createCell(3).setCellValue("199");
            exampleRow.createCell(4).setCellValue("120");
            exampleRow.createCell(5).setCellValue("80");
            exampleRow.createCell(6).setCellValue("全天");
            exampleRow.createCell(7).setCellValue("上架");
            exampleRow.createCell(8).setCellValue("2026-01-01");
            exampleRow.createCell(9).setCellValue("2026-12-31");
            exampleRow.createCell(10).setCellValue("1");

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BizException("导出模板失败: " + e.getMessage());
        }
    }

    // ===== Excel Import (columns: 门票名称, 景区项目, 票种, 价格(元), 上午库存, 下午库存, 场次, 状态, 有效期开始, 有效期结束, 退款规则ID) =====

    @Transactional
    public int importExcel(MultipartFile file) {
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            List<ScenicArea> allProjects = scenicAreaMapper.listAllAdmin();
            Map<Long, ScenicArea> projectById = allProjects.stream().collect(Collectors.toMap(ScenicArea::getId, p -> p, (a, b) -> a));
            Map<String, Long> projectIdByName = allProjects.stream()
                .filter(p -> p.getName() != null && !p.getName().isBlank())
                .collect(Collectors.toMap(p -> p.getName().trim(), ScenicArea::getId, (a, b) -> a));

            int count = 0;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = getCellString(row, 0);
                if (name == null || name.isBlank()) continue;

                String projectCell = getCellString(row, 1);
                String ticketType = getCellString(row, 2);
                String priceYuanStr = getCellString(row, 3);
                String morningStockStr = getCellString(row, 4);
                String afternoonStockStr = getCellString(row, 5);
                String timeslotStr = getCellString(row, 6);
                String statusStr = getCellString(row, 7);
                String refundRuleIdStr = getCellString(row, 10);

                List<Long> projectIds = parseProjectIds(projectCell, projectById, projectIdByName, i + 1);
                Integer morningStockQty = parseNonNegativeInt(morningStockStr, 0, i + 1, "上午库存");
                Integer afternoonStockQty = parseNonNegativeInt(afternoonStockStr, 0, i + 1, "下午库存");
                int[] timeslot = parseTimeslot(timeslotStr);
                Integer morningEnabled = timeslot[0];
                Integer afternoonEnabled = timeslot[1];

                if (morningEnabled == 0) {
                    morningStockQty = 0;
                }
                if (afternoonEnabled == 0) {
                    afternoonStockQty = 0;
                }

                Integer stockQty = morningStockQty + afternoonStockQty;
                Integer priceCent = parsePriceYuanToCent(priceYuanStr, i + 1);
                Integer status = parseStatus(statusStr);

                Ticket t = new Ticket();
                t.setScenicId(1L);
                t.setName(name.trim());
                t.setImageUrl(null);
                t.setDescription(null);
                t.setTicketType(normalizeTicketType(ticketType));
                t.setPriceCent(priceCent);
                t.setStockQty(stockQty);
                t.setMorningEnabled(morningEnabled);
                t.setAfternoonEnabled(afternoonEnabled);
                t.setValidFrom(parseLocalDateCell(row, 8, i + 1, "有效期开始"));
                t.setValidTo(parseLocalDateCell(row, 9, i + 1, "有效期结束"));
                t.setRefundRuleId(parseNullableLong(refundRuleIdStr, i + 1, "退款规则ID"));
                t.setStatus(status);

                ticketMapper.insert(t);
                bindTicketProjects(t.getId(), projectIds);
                upsertInventoryByTimeslot(t.getId(), stockQty, morningStockQty, afternoonStockQty, morningEnabled, afternoonEnabled);
                count++;
            }
            return count;
        } catch (Exception e) {
            throw new BizException("导入Excel失败: " + e.getMessage());
        }
    }

    private Integer parsePriceYuanToCent(String value, int lineNo) {
        if (value == null || value.isBlank()) {
            throw new BizException("第" + lineNo + "行价格(元)不能为空");
        }
        try {
            double yuan = Double.parseDouble(value.trim());
            if (yuan <= 0) {
                throw new BizException("第" + lineNo + "行价格(元)必须大于0");
            }
            return (int) Math.round(yuan * 100);
        } catch (NumberFormatException ex) {
            throw new BizException("第" + lineNo + "行价格(元)格式不正确");
        }
    }

    private Integer parseNonNegativeInt(String value, int defaultValue, int lineNo, String field) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            int val = Integer.parseInt(value.trim());
            if (val < 0) {
                throw new BizException("第" + lineNo + "行" + field + "不能小于0");
            }
            return val;
        } catch (NumberFormatException ex) {
            throw new BizException("第" + lineNo + "行" + field + "格式不正确");
        }
    }

    private Long parseNullableLong(String value, int lineNo, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new BizException("第" + lineNo + "行" + field + "格式不正确");
        }
    }

    private LocalDate parseLocalDateCell(Row row, int col, int lineNo, String field) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        try {
            if ((cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA) && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            return parseLocalDateText(getCellString(row, col));
        } catch (Exception ex) {
            throw new BizException("第" + lineNo + "行" + field + "格式错误，需为yyyy-MM-dd");
        }
    }

    private LocalDate parseLocalDateText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        int spaceIndex = normalized.indexOf(' ');
        if (spaceIndex > 0) {
            normalized = normalized.substring(0, spaceIndex);
        }
        normalized = normalized.replace('/', '-').replace('.', '-');
        if (normalized.matches("\\d{5,}")) {
            double serial = Double.parseDouble(normalized);
            return DateUtil.getJavaDate(serial).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyy-M-d"));
    }

    private Integer parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return 1;
        }
        String s = value.trim();
        if ("1".equals(s) || "上架".equals(s) || "启用".equals(s)) {
            return 1;
        }
        if ("0".equals(s) || "下架".equals(s) || "禁用".equals(s)) {
            return 0;
        }
        return 1;
    }

    private int[] parseTimeslot(String value) {
        if (value == null || value.isBlank()) {
            return new int[] {1, 1};
        }
        String s = value.trim();
        if ("全天".equals(s) || "上午场+下午场".equals(s) || "上午场/下午场".equals(s) || "1,1".equals(s)) {
            return new int[] {1, 1};
        }
        if ("上午".equals(s) || "上午场".equals(s) || "1,0".equals(s)) {
            return new int[] {1, 0};
        }
        if ("下午".equals(s) || "下午场".equals(s) || "0,1".equals(s)) {
            return new int[] {0, 1};
        }
        return new int[] {1, 1};
    }

    private List<Long> parseProjectIds(String cell,
                                       Map<Long, ScenicArea> projectById,
                                       Map<String, Long> projectIdByName,
                                       int lineNo) {
        if (cell == null || cell.isBlank()) {
            throw new BizException("第" + lineNo + "行景区项目不能为空");
        }
        String[] parts = cell.trim().split("[,，/、\\s]+");
        List<Long> ids = new ArrayList<>();
        for (String raw : parts) {
            String part = raw == null ? "" : raw.trim();
            if (part.isEmpty()) {
                continue;
            }
            Long id;
            if (part.matches("\\d+")) {
                id = Long.parseLong(part);
                if (!projectById.containsKey(id)) {
                    throw new BizException("第" + lineNo + "行景区项目ID不存在: " + part);
                }
            } else {
                id = projectIdByName.get(part);
                if (id == null) {
                    throw new BizException("第" + lineNo + "行景区项目名称不存在: " + part);
                }
            }
            if (!ids.contains(id)) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            throw new BizException("第" + lineNo + "行景区项目不能为空");
        }
        return ids;
    }

    private String normalizeTicketType(String ticketType) {
        if (ticketType == null || ticketType.isBlank()) {
            return "SINGLE";
        }
        String t = ticketType.trim();
        return switch (t) {
            case "单人票" -> "SINGLE";
            case "家庭票" -> "FAMILY";
            case "儿童票" -> "CHILD";
            case "学生票" -> "STUDENT";
            case "老人票" -> "SENIOR";
            default -> t;
        };
    }

    private String toTicketTypeZh(String ticketType) {
        if (ticketType == null || ticketType.isBlank()) {
            return "";
        }
        return switch (ticketType) {
            case "SINGLE" -> "单人票";
            case "FAMILY" -> "家庭票";
            case "CHILD" -> "儿童票";
            case "STUDENT" -> "学生票";
            case "SENIOR" -> "老人票";
            default -> ticketType;
        };
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private TicketDto.TicketListResp toListResp(Ticket t, List<Long> projectIds, Map<Long, String> projectNamesById) {
        String projectNames = projectIds.stream()
            .map(projectNamesById::get)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(" / "));
        return new TicketDto.TicketListResp(t.getId(), t.getScenicId(), t.getName(), t.getImageUrl(),
            t.getDescription(), t.getTicketType(),
            t.getPriceCent(), t.getStockQty(), null, null, t.getMorningEnabled(), t.getAfternoonEnabled(), t.getValidFrom(), t.getValidTo(), t.getStatus(),
            projectIds, projectNames);
    }

    private Map<Long, List<Long>> loadProjectIdsByTicket(List<Ticket> tickets) {
        List<Long> ticketIds = tickets.stream().map(Ticket::getId).toList();
        if (ticketIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<TicketProject> rels = ticketProjectMapper.listByTicketIds(ticketIds);
        Map<Long, List<Long>> byTicket = new HashMap<>();
        for (TicketProject rel : rels) {
            byTicket.computeIfAbsent(rel.getTicketId(), k -> new ArrayList<>()).add(rel.getProjectId());
        }
        return byTicket;
    }

    private Map<Long, String> loadProjectNameById(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ScenicArea> projects = scenicAreaMapper.listByIds(projectIds);
        return projects.stream().collect(Collectors.toMap(ScenicArea::getId, ScenicArea::getName));
    }

    private void validateProjectIds(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            throw new BizException("请至少选择一个景区项目");
        }
        List<Long> distinctIds = projectIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            throw new BizException("请至少选择一个景区项目");
        }
        List<ScenicArea> projects = scenicAreaMapper.listByIds(distinctIds);
        if (projects.size() != distinctIds.size()) {
            throw new BizException("景区项目不存在或已删除");
        }
    }

    private void bindTicketProjects(Long ticketId, List<Long> projectIds) {
        List<Long> ids = projectIds == null ? List.of() : projectIds.stream().filter(Objects::nonNull).distinct().toList();
        for (Long projectId : ids) {
            ticketProjectMapper.insert(ticketId, projectId);
        }
    }

    private void ensureAtLeastOneTimeslotEnabled(Integer morningEnabled, Integer afternoonEnabled) {
        int m = morningEnabled == null ? 0 : morningEnabled;
        int a = afternoonEnabled == null ? 0 : afternoonEnabled;
        if (m == 0 && a == 0) {
            throw new BizException("至少需要开放一个场次");
        }
    }

    private void upsertInventoryByTimeslot(Long ticketId,
                                           Integer stockQty,
                                           Integer morningStockQty,
                                           Integer afternoonStockQty,
                                           Integer morningEnabled,
                                           Integer afternoonEnabled) {
        int fallback = stockQty == null ? 0 : stockQty;
        int morningTarget = morningStockQty == null ? fallback : morningStockQty;
        int afternoonTarget = afternoonStockQty == null ? fallback : afternoonStockQty;
        LocalDate date = LocalDate.now();
        if ((morningEnabled == null ? 1 : morningEnabled) == 1) {
            upsertOneTimeslotInventory(ticketId, date, 1L, morningTarget);
        }
        if ((afternoonEnabled == null ? 1 : afternoonEnabled) == 1) {
            upsertOneTimeslotInventory(ticketId, date, 2L, afternoonTarget);
        }
    }

    private void upsertOneTimeslotInventory(Long ticketId, LocalDate date, Long timeslotId, int target) {
        TicketInventoryRow row = ticketInventoryMapper.lockOne(ticketId, date, timeslotId);
        if (row == null) {
            if (target > 0) {
                ticketInventoryMapper.create(ticketId, date, timeslotId, target);
            }
            return;
        }
        int minRequired = row.getSoldQty() + row.getLockedQty();
        if (target < minRequired) {
            throw new BizException("库存不能小于已售和锁定数量");
        }
        int delta = target - row.getTotalQty();
        if (delta != 0) {
            ticketInventoryMapper.adjustTotal(row.getId(), delta);
        }
    }

    private boolean isInValidDateRange(Ticket ticket, LocalDate date) {
        if (ticket.getValidFrom() != null && date.isBefore(ticket.getValidFrom())) {
            return false;
        }
        if (ticket.getValidTo() != null && date.isAfter(ticket.getValidTo())) {
            return false;
        }
        return true;
    }

    private TicketDto.InventoryResp toInventoryResp(TicketInventoryRow row) {
        int totalQty = row.getTotalQty() == null ? 0 : row.getTotalQty();
        int soldQty = row.getSoldQty() == null ? 0 : row.getSoldQty();
        int lockedQty = row.getLockedQty() == null ? 0 : row.getLockedQty();
        return new TicketDto.InventoryResp(row.getTimeslotId(), row.getTimeslotName(), totalQty, soldQty, lockedQty,
            totalQty - soldQty - lockedQty);
    }
}
