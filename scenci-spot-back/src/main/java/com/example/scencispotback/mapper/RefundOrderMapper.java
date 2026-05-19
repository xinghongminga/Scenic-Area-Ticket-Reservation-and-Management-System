package com.example.scencispotback.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// 退款映射
public interface RefundOrderMapper {

    int insertSuccess(@Param("refundNo") String refundNo,
                      @Param("orderId") Long orderId,
                      @Param("amountCent") Integer amountCent);
}
