package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.OrderTicket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 订单门票映射
public interface OrderTicketMapper {

    int insert(@Param("orderId") Long orderId,
               @Param("orderItemId") Long orderItemId,
               @Param("ticketId") Long ticketId,
               @Param("qrCode") String qrCode,
               @Param("verifyCode") String verifyCode);

    OrderTicket lockByVerifyCode(@Param("verifyCode") String verifyCode);

    OrderTicket lockByQrCode(@Param("qrCode") String qrCode);

    int markUsed(@Param("id") Long id, @Param("method") String method);

    int countUnusedByOrderId(@Param("orderId") Long orderId);

    int countUsedByOrderId(@Param("orderId") Long orderId);

    List<OrderTicket> listByOrderId(@Param("orderId") Long orderId);

    int refundUnusedByOrderId(@Param("orderId") Long orderId);

    int updateTicketByOrderId(@Param("orderId") Long orderId, @Param("ticketId") Long ticketId);
}

