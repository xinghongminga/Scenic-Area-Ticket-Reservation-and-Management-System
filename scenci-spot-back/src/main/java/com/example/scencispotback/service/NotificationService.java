package com.example.scencispotback.service;

import com.example.scencispotback.domain.Notification;
import com.example.scencispotback.mapper.NotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    /**
     * 发送通知给用户
     */
    public Notification publish(Long receiverId, String title, String content, String type) {
        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setNtype(type);
        notification.setIsRead(0);
        notification.setCreatedBy(0L);
        notification.setCreatedAt(LocalDateTime.now());

        notificationMapper.insert(notification);
        return notification;
    }

    /**
     * 发送订单支付成功通知
     */
    public Notification notifyOrderPaid(Long userId, String orderNo, String ticketName) {
        String title = "订单支付成功";
        String content = String.format("您的订单 %s (票种：%s) 支付成功，请妥善保管订单号", orderNo, ticketName);
        return publish(userId, title, content, "ORDER_PAID");
    }

    /**
     * 发送订单核验成功通知
     */
    public Notification notifyOrderVerified(Long userId, String orderNo) {
        String title = "订单已核验";
        String content = String.format("您的订单 %s 已核验，欢迎入园！", orderNo);
        return publish(userId, title, content, "ORDER_VERIFIED");
    }

    /**
     * 发送售后申请同意通知
     */
    public Notification notifyAftersaleApproved(Long userId, String orderNo) {
        String title = "售后申请已同意";
        String content = String.format("您的订单 %s 的售后申请已同意，退款将在3-5个工作日内处理", orderNo);
        return publish(userId, title, content, "AFTERSALE_APPROVED");
    }

    /**
     * 发送售后申请拒绝通知
     */
    public Notification notifyAftersaleRejected(Long userId, String orderNo, String reason) {
        String title = "售后申请已拒绝";
        String content = String.format("您的订单 %s 的售后申请已拒绝，原因：%s", orderNo, reason);
        return publish(userId, title, content, "AFTERSALE_REJECTED");
    }

    /**
     * 发送系统消息通知
     */
    public Notification notifySystemMessage(Long userId, String title, String content) {
        return publish(userId, title, content, "SYSTEM");
    }
}
