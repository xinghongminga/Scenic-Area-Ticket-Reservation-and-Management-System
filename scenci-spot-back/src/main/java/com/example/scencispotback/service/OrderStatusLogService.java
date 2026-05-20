package com.example.scencispotback.service;

import com.example.scencispotback.mapper.OrderStatusLogMapper;
import com.example.scencispotback.security.UserContext;
import org.springframework.stereotype.Service;

@Service
// 订单状态日志服务
public class OrderStatusLogService {

    private final OrderStatusLogMapper mapper;

    public OrderStatusLogService(OrderStatusLogMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 记录订单状态变更日志。
     */
    public void write(Long orderId, String fromStatus, String toStatus, String operatorType, String detailJson) {
        Long operatorId = UserContext.get() == null ? null : UserContext.get().userId();
        mapper.insert(orderId, fromStatus, toStatus, operatorType, operatorId, detailJson);
    }
}
