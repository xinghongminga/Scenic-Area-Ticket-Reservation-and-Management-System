package com.example.scencispotback.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// 订单状态日志映射
public interface OrderStatusLogMapper {

    int insert(@Param("orderId") Long orderId,
               @Param("fromStatus") String fromStatus,
               @Param("toStatus") String toStatus,
               @Param("operatorType") String operatorType,
               @Param("operatorId") Long operatorId,
               @Param("detailJson") String detailJson);
}
