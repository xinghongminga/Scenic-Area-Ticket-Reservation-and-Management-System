package com.example.scencispotback.config;

import com.example.scencispotback.domain.AftersaleRequest;
import com.example.scencispotback.mapper.TicketOrderMapper;
import com.example.scencispotback.mapper.AftersaleRequestMapper;
import com.example.scencispotback.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableScheduling
public class ScheduleConfig {

    private static final Logger log = LoggerFactory.getLogger(ScheduleConfig.class);

    @Autowired
    private AftersaleRequestMapper aftersaleRequestMapper;

    @Autowired
    private TicketOrderMapper ticketOrderMapper;

    @Autowired
    private OrderService orderService;

    @Value("${app.order.unpaid-ttl-minutes:30}")
    private long unpaidTtlMinutes;

    /**
     * 每30分钟检查一次待处理售后申请。
     * 说明：论文版仅保留定时扫描能力，实际自动退款规则由人工审核流程处理。
     */
    @Scheduled(fixedRate = 1800000)
    public void autoApproveRefund() {
        try {
            List<AftersaleRequest> submissions = aftersaleRequestMapper.listAllFiltered(null, "SUBMITTED");
            LocalDateTime now = LocalDateTime.now();
            for (AftersaleRequest req : submissions) {
                if (req.getCreatedAt() == null) {
                    continue;
                }
                // 超过72小时仍未处理则自动驳回，避免长期挂起。
                if (req.getCreatedAt().plusHours(72).isBefore(now)) {
                    aftersaleRequestMapper.updateAudit(req.getId(), "REJECTED", 0L, "系统自动关闭：超时未处理");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 每天凌晨2点检查一次逾期未核验的订单，自动标记为过期
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void markExpiredOrders() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(unpaidTtlMinutes);
            List<Long> expiredIds = ticketOrderMapper.findExpiredUnpaidIds(cutoff);
            for (Long orderId : expiredIds) {
                try {
                    orderService.closeExpiredUnpaidOrder(orderId);
                } catch (Exception ex) {
                    log.warn("Failed to close expired unpaid order id={}", orderId, ex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
