package com.example.scencispotback.service;

import com.example.scencispotback.api.aftersale.AftersaleDto;
import com.example.scencispotback.common.BizException;
import com.example.scencispotback.domain.AftersaleRequest;
import com.example.scencispotback.domain.TicketInventoryRow;
import com.example.scencispotback.domain.TicketOrder;
import com.example.scencispotback.domain.TicketOrderItem;
import com.example.scencispotback.domain.Ticket;
import com.example.scencispotback.domain.UserAccount;
import com.example.scencispotback.mapper.*;
import com.example.scencispotback.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AftersaleService {

    private final AftersaleRequestMapper aftersaleRequestMapper;
    private final TicketOrderMapper ticketOrderMapper;
    private final TicketOrderItemMapper ticketOrderItemMapper;
    private final TicketInventoryMapper ticketInventoryMapper;
    private final TicketMapper ticketMapper;
    private final OrderTicketMapper orderTicketMapper;
    private final RefundOrderMapper refundOrderMapper;
    private final OrderStatusLogService orderStatusLogService;
    private final UserAccountMapper userAccountMapper;

    public AftersaleService(AftersaleRequestMapper aftersaleRequestMapper,
                            TicketOrderMapper ticketOrderMapper,
                            TicketOrderItemMapper ticketOrderItemMapper,
                            TicketInventoryMapper ticketInventoryMapper,
                            TicketMapper ticketMapper,
                            OrderTicketMapper orderTicketMapper,
                            RefundOrderMapper refundOrderMapper,
                            OrderStatusLogService orderStatusLogService,
                            UserAccountMapper userAccountMapper) {
        this.aftersaleRequestMapper = aftersaleRequestMapper;
        this.ticketOrderMapper = ticketOrderMapper;
        this.ticketOrderItemMapper = ticketOrderItemMapper;
        this.ticketInventoryMapper = ticketInventoryMapper;
        this.ticketMapper = ticketMapper;
        this.orderTicketMapper = orderTicketMapper;
        this.refundOrderMapper = refundOrderMapper;
        this.orderStatusLogService = orderStatusLogService;
        this.userAccountMapper = userAccountMapper;
    }

    @Transactional
    public String submit(AftersaleDto.CreateReq req) {
        TicketOrder order = ticketOrderMapper.findByOrderNo(req.orderNo());
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (!order.getUserId().equals(UserContext.get().userId())) {
            throw new BizException("只能提交自己的订单售后");
        }
        if (!"PAID".equals(order.getStatus())) {
            throw new BizException("当前订单状态不支持提交售后");
        }

        String next = "REFUND".equals(req.reqType()) ? "REFUNDING" : "RESCHEDULING";
        if (!"REFUND".equals(req.reqType()) && !"RESCHEDULE".equals(req.reqType())) {
            throw new BizException("售后类型仅支持 REFUND/RESCHEDULE");
        }
        if ("RESCHEDULE".equals(req.reqType()) && (req.targetVisitDate() == null || req.targetTimeslotId() == null)) {
            throw new BizException("改签必须提供目标日期和时段");
        }
        if ("RESCHEDULE".equals(req.reqType())) {
            List<TicketOrderItem> items = ticketOrderItemMapper.listByOrderId(order.getId());
            if (items.isEmpty()) {
                throw new BizException("订单明细不存在");
            }
            TicketOrderItem first = items.get(0);
            TicketInventoryRow targetInv = ticketInventoryMapper.lockOne(first.getTicketId(), req.targetVisitDate(), req.targetTimeslotId());
            if (targetInv == null) {
                throw new BizException("目标日期时段无库存");
            }
            int remain = targetInv.getTotalQty() - targetInv.getSoldQty() - targetInv.getLockedQty();
            if (remain < first.getQty()) {
                throw new BizException("目标时段库存不足");
            }
        }

        AftersaleRequest ar = new AftersaleRequest();
        ar.setReqNo(genReqNo());
        ar.setOrderId(order.getId());
        ar.setUserId(order.getUserId());
        ar.setReqType(req.reqType());
        ar.setReason(req.reason());
        ar.setStatus("SUBMITTED");
        ar.setTargetVisitDate(req.targetVisitDate());
        ar.setTargetTimeslotId(req.targetTimeslotId());
        aftersaleRequestMapper.insert(ar);

        ticketOrderMapper.updateStatus(order.getId(), next);
        orderStatusLogService.write(order.getId(), "PAID", next, "USER", "{\"reqNo\":\"" + ar.getReqNo() + "\"}");
        return ar.getReqNo();
    }

    public List<AftersaleDto.ReqResp> myList() {
        Long userId = UserContext.get().userId();
        return aftersaleRequestMapper.listByUserId(userId).stream().map(this::toResp).toList();
    }

    public List<AftersaleDto.RescheduleOptionResp> rescheduleOptions(String orderNo) {
        TicketOrder order = ticketOrderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (!order.getUserId().equals(UserContext.get().userId())) {
            throw new BizException("只能查看自己的订单");
        }
        if (!"PAID".equals(order.getStatus())) {
            throw new BizException("当前订单状态不支持发起改签");
        }

        List<TicketOrderItem> items = ticketOrderItemMapper.listByOrderId(order.getId());
        if (items.isEmpty()) {
            throw new BizException("订单明细不存在");
        }
        TicketOrderItem first = items.get(0);
        Ticket ticket = ticketMapper.findById(first.getTicketId());
        if (ticket == null) {
            throw new BizException("门票不存在");
        }

        boolean morningEnabled = (ticket.getMorningEnabled() == null ? 1 : ticket.getMorningEnabled()) == 1;
        boolean afternoonEnabled = (ticket.getAfternoonEnabled() == null ? 1 : ticket.getAfternoonEnabled()) == 1;
        LocalDate startDate = LocalDate.now();
        Map<LocalDate, List<AftersaleDto.RescheduleTimeslotResp>> grouped = new LinkedHashMap<>();
        for (TicketInventoryRow row : ticketInventoryMapper.listAvailableByTicketFromDate(first.getTicketId(), startDate)) {
            if (ticket.getValidFrom() != null && row.getVisitDate().isBefore(ticket.getValidFrom())) {
                continue;
            }
            if (ticket.getValidTo() != null && row.getVisitDate().isAfter(ticket.getValidTo())) {
                continue;
            }
            if (!morningEnabled && Long.valueOf(1L).equals(row.getTimeslotId())) {
                continue;
            }
            if (!afternoonEnabled && Long.valueOf(2L).equals(row.getTimeslotId())) {
                continue;
            }
            int remain = row.getTotalQty() - row.getSoldQty() - row.getLockedQty();
            if (remain <= 0) {
                continue;
            }
            grouped.computeIfAbsent(row.getVisitDate(), k -> new ArrayList<>())
                .add(new AftersaleDto.RescheduleTimeslotResp(row.getTimeslotId(), row.getTimeslotName(), remain));
        }

        return grouped.entrySet().stream()
            .map(e -> new AftersaleDto.RescheduleOptionResp(e.getKey(), e.getValue()))
            .toList();
    }

    public List<AftersaleDto.ReqResp> allList(AftersaleDto.QueryReq query) {
        return aftersaleRequestMapper.listAllFiltered(query.userPhone(), query.status()).stream().map(this::toResp).toList();
    }

    @Transactional
    public void updateByAuditor(String reqNo, AftersaleDto.UpdateReq req) {
        AftersaleRequest ar = aftersaleRequestMapper.lockByReqNo(reqNo);
        if (ar == null) {
            throw new BizException("售后单不存在");
        }
        if (!"SUBMITTED".equals(ar.getStatus())) {
            throw new BizException("仅处理中售后单可编辑");
        }
        aftersaleRequestMapper.updateContent(ar.getId(), req == null ? ar.getReason() : req.reason(),
            req == null ? ar.getTargetVisitDate() : req.targetVisitDate(),
            req == null ? ar.getTargetTimeslotId() : req.targetTimeslotId());
    }

    @Transactional
    public void deleteByAuditor(String reqNo) {
        AftersaleRequest ar = aftersaleRequestMapper.lockByReqNo(reqNo);
        if (ar == null) {
            throw new BizException("售后单不存在");
        }
        TicketOrder order = ticketOrderMapper.findById(ar.getOrderId());
        if (order != null && "SUBMITTED".equals(ar.getStatus())) {
            String current = order.getStatus();
            if ("REFUNDING".equals(current) || "RESCHEDULING".equals(current)) {
                ticketOrderMapper.updateStatus(order.getId(), "PAID");
                orderStatusLogService.write(order.getId(), current, "PAID", "STAFF", "{\"action\":\"aftersale_delete\"}");
            }
        }
        aftersaleRequestMapper.deleteById(ar.getId());
    }

    @Transactional
    public void approve(String reqNo, String comment) {
        Long auditorId = UserContext.get().userId();
        AftersaleRequest ar = aftersaleRequestMapper.lockByReqNo(reqNo);
        if (ar == null) {
            throw new BizException("售后单不存在");
        }
        if (!"SUBMITTED".equals(ar.getStatus())) {
            throw new BizException("该售后单已处理");
        }

        TicketOrder order = ticketOrderMapper.findById(ar.getOrderId());
        if (order == null) {
            throw new BizException("关联订单不存在");
        }
        List<TicketOrderItem> items = ticketOrderItemMapper.listByOrderId(order.getId());
        if (items.isEmpty()) {
            throw new BizException("订单明细不存在");
        }
        TicketOrderItem first = items.get(0);

        if ("REFUND".equals(ar.getReqType())) {
            int used = orderTicketMapper.countUsedByOrderId(order.getId());
            if (used > 0) {
                throw new BizException("已使用订单不支持整单退款");
            }

            TicketInventoryRow inv = ticketInventoryMapper.lockOne(first.getTicketId(), order.getVisitDate(), order.getTimeslotId());
            if (inv != null) {
                ticketInventoryMapper.subSold(inv.getId(), first.getQty());
            }
            orderTicketMapper.refundUnusedByOrderId(order.getId());
            refundOrderMapper.insertSuccess(genRefundNo(), order.getId(), order.getTotalAmountCent());

            ticketOrderMapper.updateStatus(order.getId(), "REFUNDED");
            aftersaleRequestMapper.updateAudit(ar.getId(), "DONE", auditorId, comment == null ? "审核通过-已退款" : comment);
            orderStatusLogService.write(order.getId(), "REFUNDING", "REFUNDED", "STAFF", "{\"reqNo\":\"" + reqNo + "\"}");
            return;
        }

        TicketInventoryRow oldInv = ticketInventoryMapper.lockOne(first.getTicketId(), order.getVisitDate(), order.getTimeslotId());
        TicketInventoryRow newInv = ticketInventoryMapper.lockOne(first.getTicketId(), ar.getTargetVisitDate(), ar.getTargetTimeslotId());
        if (newInv == null) {
            throw new BizException("目标日期时段无库存");
        }
        int remain = newInv.getTotalQty() - newInv.getSoldQty() - newInv.getLockedQty();
        if (remain < first.getQty()) {
            throw new BizException("目标时段库存不足");
        }

        if (oldInv != null) {
            ticketInventoryMapper.subSold(oldInv.getId(), first.getQty());
        }
        ticketInventoryMapper.addSold(newInv.getId(), first.getQty());
        ticketOrderMapper.updateVisit(order.getId(), ar.getTargetVisitDate(), ar.getTargetTimeslotId());
        ticketOrderMapper.updateStatus(order.getId(), "RESCHEDULED");

        aftersaleRequestMapper.updateAudit(ar.getId(), "DONE", auditorId, comment == null ? "审核通过-已改签" : comment);
        orderStatusLogService.write(order.getId(), "RESCHEDULING", "RESCHEDULED", "STAFF", "{\"reqNo\":\"" + reqNo + "\"}");
    }

    @Transactional
    public void reject(String reqNo, String comment) {
        Long auditorId = UserContext.get().userId();
        AftersaleRequest ar = aftersaleRequestMapper.lockByReqNo(reqNo);
        if (ar == null) {
            throw new BizException("售后单不存在");
        }
        if (!"SUBMITTED".equals(ar.getStatus())) {
            throw new BizException("该售后单已处理");
        }

        String from = "REFUND".equals(ar.getReqType()) ? "REFUNDING" : "RESCHEDULING";
        ticketOrderMapper.updateStatus(ar.getOrderId(), "PAID");
        aftersaleRequestMapper.updateAudit(ar.getId(), "REJECTED", auditorId, comment == null ? "审核拒绝" : comment);
        orderStatusLogService.write(ar.getOrderId(), from, "PAID", "STAFF", "{\"reqNo\":\"" + reqNo + "\"}");
    }

    private AftersaleDto.ReqResp toResp(AftersaleRequest r) {
        TicketOrder order = ticketOrderMapper.findById(r.getOrderId());
        String orderNo = order == null ? null : order.getOrderNo();
        UserAccount user = (order == null || order.getUserId() == null) ? null : userAccountMapper.findById(order.getUserId());
        return new AftersaleDto.ReqResp(r.getReqNo(),
            orderNo,
            user == null ? null : user.getNickname(),
            user == null ? null : user.getPhone(),
            r.getReqType(), r.getStatus(), r.getReason(),
            r.getTargetVisitDate(), r.getTargetTimeslotId(), r.getCreatedAt());
    }

    private String genReqNo() {
        return "AR" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
            + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    private String genRefundNo() {
        return "RF" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
            + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}
