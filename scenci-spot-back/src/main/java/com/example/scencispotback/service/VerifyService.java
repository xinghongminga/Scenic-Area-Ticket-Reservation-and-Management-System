package com.example.scencispotback.service;

import com.example.scencispotback.api.verify.VerifyDto;
import com.example.scencispotback.common.BizException;
import com.example.scencispotback.domain.OrderTicket;
import com.example.scencispotback.domain.TicketOrder;
import com.example.scencispotback.mapper.OrderTicketMapper;
import com.example.scencispotback.mapper.TicketOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerifyService {

    private final OrderTicketMapper orderTicketMapper;
    private final TicketOrderMapper ticketOrderMapper;
    private final OrderStatusLogService orderStatusLogService;

    public VerifyService(OrderTicketMapper orderTicketMapper,
                        TicketOrderMapper ticketOrderMapper,
                        OrderStatusLogService orderStatusLogService) {
        this.orderTicketMapper = orderTicketMapper;
        this.ticketOrderMapper = ticketOrderMapper;
        this.orderStatusLogService = orderStatusLogService;
    }

    @Transactional
    public VerifyDto.VerifyResp verifyByCode(VerifyDto.VerifyByCodeReq req) {
        OrderTicket orderTicket = orderTicketMapper.lockByVerifyCode(req.verifyCode());
        if (orderTicket == null) {
            throw new BizException("核验码不存在");
        }
        return doVerify(orderTicket, req.method() == null ? "MANUAL" : req.method());
    }

    @Transactional
    public VerifyDto.VerifyResp verifyByQr(VerifyDto.VerifyByQrReq req) {
        OrderTicket orderTicket = orderTicketMapper.lockByQrCode(req.qrCode());
        if (orderTicket == null) {
            throw new BizException("二维码不存在");
        }
        return doVerify(orderTicket, req.method() == null ? "QR" : req.method());
    }

    private VerifyDto.VerifyResp doVerify(OrderTicket orderTicket, String method) {
        if (!"UNUSED".equals(orderTicket.getStatus())) {
            throw new BizException("该电子票已核销或已退款");
        }
        int updated = orderTicketMapper.markUsed(orderTicket.getId(), method);
        if (updated == 0) {
            throw new BizException("核验失败");
        }

        TicketOrder order = ticketOrderMapper.findById(orderTicket.getOrderId());
        if (order == null) {
            throw new BizException("订单不存在");
        }

        int unused = orderTicketMapper.countUnusedByOrderId(order.getId());
        String nextOrderStatus = unused == 0 ? "USED" : "PAID";
        if (!nextOrderStatus.equals(order.getStatus())) {
            ticketOrderMapper.updateStatus(order.getId(), nextOrderStatus);
            orderStatusLogService.write(order.getId(), order.getStatus(), nextOrderStatus, "STAFF", "{\"action\":\"verify\"}");
        }

        return new VerifyDto.VerifyResp(order.getOrderNo(), "USED", nextOrderStatus);
    }
}
