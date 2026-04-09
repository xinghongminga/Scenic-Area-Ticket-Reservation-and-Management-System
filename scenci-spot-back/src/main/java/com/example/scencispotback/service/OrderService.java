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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
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

    private final TicketMapper ticketMapper;
    private final TicketInventoryMapper inventoryMapper;
    private final TicketOrderMapper orderMapper;
    private final TicketOrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final OrderTicketMapper orderTicketMapper;
    private final OrderStatusLogService orderStatusLogService;
    private final NotificationService notificationService;

    public OrderService(TicketMapper ticketMapper,
                        TicketInventoryMapper inventoryMapper,
                        TicketOrderMapper orderMapper,
                        TicketOrderItemMapper orderItemMapper,
                        PaymentMapper paymentMapper,
                        OrderTicketMapper orderTicketMapper,
                        OrderStatusLogService orderStatusLogService,
                        NotificationService notificationService) {
        this.ticketMapper = ticketMapper;
        this.inventoryMapper = inventoryMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentMapper = paymentMapper;
        this.orderTicketMapper = orderTicketMapper;
        this.orderStatusLogService = orderStatusLogService;
        this.notificationService = notificationService;
    }

    /**
     * 创建订单：校验门票 → 锁定库存 → 生成未支付订单
     */
    @Transactional
    public OrderDto.CreateOrderResp create(OrderDto.CreateOrderReq req) {
        Long userId = UserContext.get().userId();
        Ticket ticket = ticketMapper.findById(req.ticketId());
        if (ticket == null || ticket.getStatus() == 0) {
            throw new BizException("门票不存在或未上架");
        }

        // 悲观锁查询库存，防止并发超卖
        TicketInventoryRow inv = inventoryMapper.lockOne(req.ticketId(), req.visitDate(), req.timeslotId());
        if (inv == null) {
            throw new BizException("该日期时段暂无库存");
        }

        int remain = inv.getTotalQty() - inv.getSoldQty() - inv.getLockedQty();
        if (remain < req.qty()) {
            throw new BizException("库存不足");
        }
        inventoryMapper.addLocked(inv.getId(), req.qty());

        // 计算总价并创建订单
        int amount = ticket.getPriceCent() * req.qty();
        TicketOrder order = new TicketOrder();
        order.setOrderNo(genOrderNo());
        order.setScenicId(ticket.getScenicId());
        order.setUserId(userId);
        order.setVisitDate(req.visitDate());
        order.setTimeslotId(req.timeslotId());
        order.setTotalAmountCent(amount);
        order.setStatus("UNPAID");
        orderMapper.insert(order);

        // 插入订单明细
        TicketOrderItem item = new TicketOrderItem();
        item.setOrderId(order.getId());
        item.setTicketId(ticket.getId());
        item.setTicketName(ticket.getName());
        item.setUnitPriceCent(ticket.getPriceCent());
        item.setQty(req.qty());
        item.setAmountCent(amount);
        orderItemMapper.insert(item);

        orderStatusLogService.write(order.getId(), null, "UNPAID", "USER", "{\"action\":\"create\"}");
        return new OrderDto.CreateOrderResp(order.getOrderNo(), amount, order.getStatus());
    }

    /**
     * 订单支付：锁库存转已售 → 生成支付记录 → 生成电子票二维码
     */
    @Transactional
    public OrderDto.PayResp pay(String orderNo) {
        TicketOrder order = orderMapper.findByOrderNo(orderNo);
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

        List<TicketOrderItem> items = orderItemMapper.listByOrderId(order.getId());
        if (items.isEmpty()) {
            throw new BizException("订单明细不存在");
        }

        // 库存校验并正式扣减
        TicketOrderItem first = items.get(0);
        TicketInventoryRow inv = inventoryMapper.lockOne(first.getTicketId(), order.getVisitDate(), order.getTimeslotId());
        if (inv == null || inv.getLockedQty() < first.getQty()) {
            throw new BizException("库存锁定异常");
        }
        inventoryMapper.lockToSold(inv.getId(), first.getQty());

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

        // 未支付订单释放锁定库存
        if ("UNPAID".equals(order.getStatus())) {
            List<TicketOrderItem> items = orderItemMapper.listByOrderId(order.getId());
            for (TicketOrderItem item : items) {
                TicketInventoryRow inv = inventoryMapper.lockOne(item.getTicketId(), order.getVisitDate(), order.getTimeslotId());
                if (inv != null && inv.getLockedQty() > 0) {
                    inventoryMapper.subLocked(inv.getId(), Math.min(inv.getLockedQty(), item.getQty()));
                }
            }
        }

        orderMapper.updateStatus(order.getId(), "DELETED");
        orderStatusLogService.write(order.getId(), order.getStatus(), "DELETED", "USER", "{\"action\":\"delete\"}");
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

    /**
     * 生成唯一订单号
     */
    private String genOrderNo() {
        return "OD" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(java.time.LocalDateTime.now())
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}