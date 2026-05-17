package com.example.scencispotback.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// 退款映射
public interface RefundOrderMapper {

    @Insert("insert into refund_order(refund_no, order_id, amount_cent, status) values(#{refundNo}, #{orderId}, #{amountCent}, 'SUCCESS')")
    int insertSuccess(@Param("refundNo") String refundNo,
                      @Param("orderId") Long orderId,
                      @Param("amountCent") Integer amountCent);
}
