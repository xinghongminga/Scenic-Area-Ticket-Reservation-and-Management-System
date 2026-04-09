package com.example.scencispotback.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentMapper {

    @Insert("insert into payment(order_id, pay_no, channel, status, amount_cent, gateway_trade_no, mock_payload) " +
        "values(#{orderId}, #{payNo}, 'VIRTUAL', 'SUCCESS', #{amountCent}, #{gatewayTradeNo}, #{payload})")
    int insertSuccess(@Param("orderId") Long orderId,
                      @Param("payNo") String payNo,
                      @Param("amountCent") Integer amountCent,
                      @Param("gatewayTradeNo") String gatewayTradeNo,
                      @Param("payload") String payload);
}
