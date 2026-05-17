package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.OrderTicket;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
// 订单门票映射
public interface OrderTicketMapper {

    @Insert("insert into order_ticket(order_id, order_item_id, ticket_id, qr_code, verify_code, status) " +
        "values(#{orderId}, #{orderItemId}, #{ticketId}, #{qrCode}, #{verifyCode}, 'UNUSED')")
    int insert(@Param("orderId") Long orderId,
               @Param("orderItemId") Long orderItemId,
               @Param("ticketId") Long ticketId,
               @Param("qrCode") String qrCode,
               @Param("verifyCode") String verifyCode);

    @Select("select * from order_ticket where verify_code=#{verifyCode} for update")
    OrderTicket lockByVerifyCode(@Param("verifyCode") String verifyCode);

    @Select("select * from order_ticket where qr_code=#{qrCode} for update")
    OrderTicket lockByQrCode(@Param("qrCode") String qrCode);

    @Update("update order_ticket set status='USED', used_at=now(), verify_method=#{method} where id=#{id} and status='UNUSED'")
    int markUsed(@Param("id") Long id, @Param("method") String method);

    @Select("select count(1) from order_ticket where order_id=#{orderId} and status='UNUSED'")
    int countUnusedByOrderId(@Param("orderId") Long orderId);

    @Select("select count(1) from order_ticket where order_id=#{orderId} and status='USED'")
    int countUsedByOrderId(@Param("orderId") Long orderId);

    @Select("select * from order_ticket where order_id=#{orderId} order by id asc")
    List<OrderTicket> listByOrderId(@Param("orderId") Long orderId);

    @Update("update order_ticket set status='REFUNDED' where order_id=#{orderId} and status='UNUSED'")
    int refundUnusedByOrderId(@Param("orderId") Long orderId);

    @Update("update order_ticket set ticket_id=#{ticketId} where order_id=#{orderId}")
    int updateTicketByOrderId(@Param("orderId") Long orderId, @Param("ticketId") Long ticketId);
}

