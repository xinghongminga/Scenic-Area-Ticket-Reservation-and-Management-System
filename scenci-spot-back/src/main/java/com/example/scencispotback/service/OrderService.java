package com.example.scencispotback.service;

import com.example.scencispotback.api.order.OrderDto;
import com.example.scencispotback.common.BizException;
import com.example.scencispotback.domain.OrderTicket;
import com.example.scencispotback.domain.Ticket;
import com.example.scencispotback.domain.TicketInventoryRow;
import com.example.scencispotback.domain.Notification;
import com.example.scencispotback.domain.TicketOrder;
import com.example.scencispotback.domain.TicketOrderItem;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.example.scencispotback.mapper.*;
import com.example.scencispotback.security.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 景区门票订单业务服务
 * 包含：创建订单、支付、查询、删除、二维码生成、订单导出
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final int BOOKABLE_DAYS = 14;

    private final TicketMapper ticketMapper;
    private final TicketInventoryMapper inventoryMapper;
    private final TicketOrderMapper orderMapper;
    private final TicketOrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final OrderTicketMapper orderTicketMapper;
    private final OrderStatusLogService orderStatusLogService;
    private final NotificationService notificationService;
    private final InventoryOptimisticService inventoryOptimisticService;
    private final long unpaidTtlMinutes;

    public OrderService(TicketMapper ticketMapper,
                        TicketInventoryMapper inventoryMapper,
                        TicketOrderMapper orderMapper,
                        TicketOrderItemMapper orderItemMapper,
                        PaymentMapper paymentMapper,
                        OrderTicketMapper orderTicketMapper,
                        OrderStatusLogService orderStatusLogService,
                        NotificationService notificationService,
                        InventoryOptimisticService inventoryOptimisticService,
                        @Value("${app.order.unpaid-ttl-minutes:30}") long unpaidTtlMinutes) {
        this.ticketMapper = ticketMapper;
        this.inventoryMapper = inventoryMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentMapper = paymentMapper;
        this.orderTicketMapper = orderTicketMapper;
        this.orderStatusLogService = orderStatusLogService;
        this.notificationService = notificationService;
        this.inventoryOptimisticService = inventoryOptimisticService;
        this.unpaidTtlMinutes = unpaidTtlMinutes;
    }

    /**
     * 创建订单：Redis 原子预扣库存 + DB 条件更新 + 生成未支付订单
     */
    @Transactional
    public OrderDto.CreateOrderResp create(OrderDto.CreateOrderReq req) {
        Long userId = UserContext.get().userId();
        Ticket ticket = ticketMapper.findById(req.ticketId());
        if (ticket == null || ticket.getStatus() == 0) {
            throw new BizException("门票不存在或未上架");
        }
        validateVisitDate(req.visitDate(), ticket);

        TicketInventoryRow inv = inventoryMapper.findOne(req.ticketId(), req.visitDate(), req.timeslotId());
        if (inv == null || inv.getStatus() == null || inv.getStatus() != 1) {
            throw new BizException("该日期时段暂无库存");
        }

        boolean redisReserved = inventoryOptimisticService.reserve(
            req.ticketId(), req.visitDate(), req.timeslotId(), req.qty(), inv
        );
        if (!redisReserved) {
            throw new BizException("库存不足");
        }

        boolean needCompensate = true;

        try {
            int dbUpdated = inventoryMapper.tryAddLocked(req.ticketId(), req.visitDate(), req.timeslotId(), req.qty());
            if (dbUpdated == 0) {
                inventoryOptimisticService.release(req.ticketId(), req.visitDate(), req.timeslotId(), req.qty());
                needCompensate = false;
                throw new BizException("库存不足");
            }

            int amount = ticket.getPriceCent() * req.qty();
            TicketOrder order = new TicketOrder();
            order.setOrderNo(genOrderNo());
            order.setScenicId(ticket.getScenicId());
            order.setUserId(userId);
            order.setVisitDate(req.visitDate());
            order.setTimeslotId(req.timeslotId());
            order.setTotalAmountCent(amount);
            order.setStatus("UNPAID");
            order.setCloseReason(null);
            orderMapper.insert(order);

            TicketOrderItem item = new TicketOrderItem();
            item.setOrderId(order.getId());
            item.setTicketId(ticket.getId());
            item.setTicketName(ticket.getName());
            item.setUnitPriceCent(ticket.getPriceCent());
            item.setQty(req.qty());
            item.setAmountCent(amount);
            orderItemMapper.insert(item);

            orderStatusLogService.write(order.getId(), null, "UNPAID", "USER", "{\"action\":\"create\"}");
            needCompensate = false;
            return new OrderDto.CreateOrderResp(order.getOrderNo(), amount, order.getStatus());
        } catch (RuntimeException e) {
            if (needCompensate) {
                inventoryOptimisticService.release(req.ticketId(), req.visitDate(), req.timeslotId(), req.qty());
            }
            throw e;
        }
    }

    /**
     * 订单支付：锁库存转已售 → 生成支付记录 → 生成电子票二维码
     */
    @Transactional
    public OrderDto.PayResp pay(String orderNo) {
        TicketOrder order = orderMapper.findByOrderNoForUpdate(orderNo);
        if (order == null) {
            throw new BizException("订单不存在");
        }

        // 权限校验
        String role = UserContext.get().role();
        if (!order.getUserId().equals(UserContext.get().userId()) && !"ADMIN".equals(role)) {
            throw new BizException("无权限操作该订单");
        }
        if (!"UNPAID".equals(order.getStatus())) {
            throw new BizException("订单状态不允许支付");
        }

        LocalDateTime now = LocalDateTime.now();
        if (isExpired(order, now)) {
            closeExpiredUnpaidOrderLocked(order, now);
            throw new BizException("订单已超时关闭");
        }

        List<TicketOrderItem> items = orderItemMapper.listByOrderId(order.getId());
        if (items.isEmpty()) {
            throw new BizException("订单明细不存在");
        }

        // 订单支付只需要把已锁库存转已售，不再走行级悲观锁
        TicketOrderItem first = items.get(0);
        int moved = inventoryMapper.tryLockToSold(first.getTicketId(), order.getVisitDate(), order.getTimeslotId(), first.getQty());
        if (moved == 0) {
            throw new BizException("库存锁定异常");
        }

        // 模拟支付记录
        String payNo = "PAY" + System.currentTimeMillis();
        String gatewayNo = "VTX" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        paymentMapper.insertSuccess(order.getId(), payNo, order.getTotalAmountCent(), gatewayNo, "{\"mock\":true}");
        orderMapper.updateStatus(order.getId(), "PAID");

        // 按购买数量生成电子票
        List<String> qrCodes = new ArrayList<>();
        for (int i = 0; i < first.getQty(); i++) {
            String qr = "QR-" + order.getOrderNo() + "-" + (i + 1) + "-" + UUID.randomUUID().toString().substring(0, 8);
            String verifyCode = "VC" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            orderTicketMapper.insert(order.getId(), first.getId(), first.getTicketId(), qr, verifyCode);
            qrCodes.add(qr);
        }

        orderStatusLogService.write(order.getId(), "UNPAID", "PAID", "USER", "{\"action\":\"virtual_pay\"}");
        Notification notification = notificationService.notifyOrderPaid(order.getUserId(), order.getOrderNo(), first.getTicketName());

        return new OrderDto.PayResp(orderNo,
                payNo,
                gatewayNo,
                order.getTotalAmountCent(),
                qrCodes,
                notification == null ? null : notification.getId(),
                notification == null ? null : notification.getTitle(),
                notification == null ? null : notification.getContent());
    }

    /**
     * 查询当前用户的所有订单
     */
    public List<OrderDto.OrderResp> myOrders() {
        Long userId = UserContext.get().userId();
        return orderMapper.findMyOrders(userId).stream().map(this::toOrderResp).toList();
    }

    /**
     * 订单详情：主信息 + 明细 + 电子票信息
     */
    public OrderDto.OrderDetailResp detail(String orderNo) {
        TicketOrder order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        validateOrderViewPermission(order);

        List<OrderDto.OrderItemResp> items = orderItemMapper.listByOrderId(order.getId()).stream().map(i ->
                new OrderDto.OrderItemResp(i.getTicketId(), i.getTicketName(), i.getUnitPriceCent(), i.getQty(), i.getAmountCent())
        ).toList();

        List<OrderDto.OrderTicketResp> tickets = orderTicketMapper.listByOrderId(order.getId()).stream().map(t ->
                new OrderDto.OrderTicketResp(t.getQrCode(), t.getVerifyCode(), t.getStatus())
        ).toList();

        return new OrderDto.OrderDetailResp(toOrderResp(order), items, tickets);
    }

    /**
     * 根据核销码生成二维码图片流
     */
    public byte[] renderTicketQrImage(String orderNo, String verifyCode) {
        TicketOrder order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        validateOrderViewPermission(order);

        OrderTicket ticket = orderTicketMapper.listByOrderId(order.getId()).stream()
                .filter(item -> verifyCode.equals(item.getVerifyCode()))
                .findFirst()
                .orElseThrow(() -> new BizException("电子票不存在"));

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(ticket.getQrCode(), BarcodeFormat.QR_CODE, 320, 320);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new BizException("生成二维码失败: " + e.getMessage());
        }
    }

    /**
     * 删除自己的订单：未支付则释放库存，已核销不可删
     */
    @Transactional
    public void deleteMyOrder(String orderNo) {
        Long userId = UserContext.get().userId();
        String role = UserContext.get().role();

        TicketOrder order = "ADMIN".equals(role)
                ? orderMapper.findByOrderNo(orderNo)
                : orderMapper.findByOrderNoAndUserId(orderNo, userId);

        if (order == null) {
            throw new BizException("订单不存在");
        }
        if ("USED".equals(order.getStatus())) {
            throw new BizException("已核销订单不允许删除");
        }

        // 未支付订单释放锁定库存，并归还 Redis 可售余量
        if ("UNPAID".equals(order.getStatus())) {
            List<TicketOrderItem> items = orderItemMapper.listByOrderId(order.getId());
            for (TicketOrderItem item : items) {
                int released = inventoryMapper.trySubLocked(item.getTicketId(), order.getVisitDate(), order.getTimeslotId(), item.getQty());
                if (released == 0) {
                    throw new BizException("库存释放异常");
                }
                inventoryOptimisticService.release(item.getTicketId(), order.getVisitDate(), order.getTimeslotId(), item.getQty());
            }
        }

        orderMapper.updateStatus(order.getId(), "DELETED");
        orderStatusLogService.write(order.getId(), order.getStatus(), "DELETED", "USER", "{\"action\":\"delete\"}");
    }

    /**
     * 定时任务调用：关闭已超时未支付订单，并归还库存。
     */
    @Transactional
    public boolean closeExpiredUnpaidOrder(Long orderId) {
        TicketOrder order = orderMapper.findByIdForUpdate(orderId);
        if (order == null || !"UNPAID".equals(order.getStatus())) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (!isExpired(order, now)) {
            return false;
        }

        return closeExpiredUnpaidOrderLocked(order, now);
    }

    /**
     * 管理员条件查询所有订单
     */
    public List<OrderDto.OrderResp> allOrders(OrderDto.OrderQuery query) {
        return orderMapper.findAll(query.status(),
                        query.userPhone(), query.visitDate(),
                        query.createdAfter(), query.createdBefore())
                .stream().map(this::toOrderResp).toList();
    }

    /**
     * 导出订单为 CSV 格式
     */
    public String exportOrdersCsv(OrderDto.OrderQuery query) {
        List<OrderDto.OrderResp> orders = allOrders(query);
        StringBuilder sb = new StringBuilder();
        sb.append("订单号,用户昵称,手机号,姓名,身份证号,景区ID,出行日期,金额(分),状态,下单时间,门票名称\n");

        for (OrderDto.OrderResp o : orders) {
            sb.append(csv(o.orderNo())).append(",")
                    .append(csv(o.userNickname())).append(",")
                    .append(csv(o.userPhone())).append(",")
                    .append(csv(o.userFullName())).append(",")
                    .append(csv(o.userIdCardNo())).append(",")
                    .append(o.scenicId()).append(",")
                    .append(o.visitDate()).append(",")
                    .append(o.totalAmountCent()).append(",")
                    .append(csv(o.status())).append(",")
                    .append(o.createdAt()).append(",")
                    .append(csv(o.ticketName())).append("\n");
        }
        return sb.toString();
    }

    /**
     * 领域对象转前端DTO
     */
    private OrderDto.OrderResp toOrderResp(TicketOrder o) {
        return new OrderDto.OrderResp(o.getOrderNo(), o.getUserId(),
                o.getUserNickname(), o.getUserPhone(), o.getUserFullName(), o.getUserIdCardNo(),
                o.getScenicId(), o.getVisitDate(), o.getTimeslotId(),
                o.getTotalAmountCent(), o.getStatus(), o.getCreatedAt(),
                o.getTicketName(), o.getTicketImageUrl());
    }

    /**
     * 订单查看权限校验
     */
    private void validateOrderViewPermission(TicketOrder order) {
        String role = UserContext.get().role();
        if (!order.getUserId().equals(UserContext.get().userId()) && !"ADMIN".equals(role)) {
            throw new BizException("无权限查看该订单");
        }
    }

    /**
     * CSV 内容转义，防止引号、逗号错乱
     */
    private String csv(String val) {
        if (val == null) return "";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }

    private boolean isExpired(TicketOrder order, LocalDateTime now) {
        if (order == null || order.getCreatedAt() == null) {
            return false;
        }
        return !order.getCreatedAt().plusMinutes(unpaidTtlMinutes).isAfter(now);
    }

    private boolean closeExpiredUnpaidOrderLocked(TicketOrder order, LocalDateTime now) {
        if (order == null || !"UNPAID".equals(order.getStatus()) || !isExpired(order, now)) {
            return false;
        }

        List<TicketOrderItem> items = orderItemMapper.listByOrderId(order.getId());
        for (TicketOrderItem item : items) {
            int released = inventoryMapper.trySubLocked(item.getTicketId(), order.getVisitDate(), order.getTimeslotId(), item.getQty());
            if (released == 0) {
                throw new BizException("库存释放异常");
            }
            inventoryOptimisticService.release(item.getTicketId(), order.getVisitDate(), order.getTimeslotId(), item.getQty());
        }

        orderMapper.updateStatusAndReason(order.getId(), "EXPIRED", "TIMEOUT");
        orderStatusLogService.write(order.getId(), "UNPAID", "EXPIRED", "SYSTEM", "{\"action\":\"timeout_close\",\"ttlMinutes\":" + unpaidTtlMinutes + "}");
        log.info("Expired unpaid order closed: orderNo={}, orderId={}", order.getOrderNo(), order.getId());
        return true;
    }

    /**
     * 生成唯一订单号
     */
    private String genOrderNo() {
        return "OD" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(java.time.LocalDateTime.now())
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    private void validateVisitDate(LocalDate visitDate, Ticket ticket) {
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(BOOKABLE_DAYS - 1L);
        if (visitDate.isBefore(today) || visitDate.isAfter(maxDate)) {
            throw new BizException("仅支持预订未来14天内的日期");
        }
        if (ticket.getValidDate() != null && !ticket.getValidDate().equals(visitDate)) {
            throw new BizException("出行日期不在门票有效期内");
        }
    }
}