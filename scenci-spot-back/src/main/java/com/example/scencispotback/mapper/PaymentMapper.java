package com.example.scencispotback.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// 支付映射器
public interface PaymentMapper {

    int insertSuccess(@Param("orderId") Long orderId,
                      @Param("payNo") String payNo,
                      @Param("amountCent") Integer amountCent,
                      @Param("gatewayTradeNo") String gatewayTradeNo,
                      @Param("payload") String payload);
}
