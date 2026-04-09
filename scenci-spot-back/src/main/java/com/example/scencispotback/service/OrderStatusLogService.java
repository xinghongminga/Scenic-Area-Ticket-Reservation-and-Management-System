package com.example.scencispotback.service;

import com.example.scencispotback.mapper.OrderStatusLogMapper;
import com.example.scencispotback.security.UserContext;
import org.springframework.stereotype.Service;

@Service
public class OrderStatusLogService {

    private final OrderStatusLogMapper mapper;

    public OrderStatusLogService(OrderStatusLogMapper mapper) {
        this.mapper = mapper;
    }

    public void write(Long orderId, String fromStatus, String toStatus, String operatorType, String detailJson) {
        Long operatorId = UserContext.get() == null ? null : UserContext.get().userId();
        mapper.insert(orderId, fromStatus, toStatus, operatorType, operatorId, detailJson);
    }
}
