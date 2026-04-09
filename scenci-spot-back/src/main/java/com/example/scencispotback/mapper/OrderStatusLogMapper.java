package com.example.scencispotback.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderStatusLogMapper {

    @Insert("insert into order_status_log(order_id, from_status, to_status, operator_type, operator_id, detail_json) " +
        "values(#{orderId}, #{fromStatus}, #{toStatus}, #{operatorType}, #{operatorId}, #{detailJson})")
    int insert(@Param("orderId") Long orderId,
               @Param("fromStatus") String fromStatus,
               @Param("toStatus") String toStatus,
               @Param("operatorType") String operatorType,
               @Param("operatorId") Long operatorId,
               @Param("detailJson") String detailJson);
}
