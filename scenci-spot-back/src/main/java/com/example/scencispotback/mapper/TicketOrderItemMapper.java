package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.TicketOrderItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
// 订单项映射
public interface TicketOrderItemMapper {

    @Insert("insert into ticket_order_item(order_id, ticket_id, ticket_name, unit_price_cent, qty, amount_cent) " +
        "values(#{orderId}, #{ticketId}, #{ticketName}, #{unitPriceCent}, #{qty}, #{amountCent})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TicketOrderItem item);

    @Select("select * from ticket_order_item where order_id=#{orderId}")
    List<TicketOrderItem> listByOrderId(@Param("orderId") Long orderId);

    @Select("select count(1) from ticket_order_item where ticket_id=#{ticketId}")
    int countByTicketId(@Param("ticketId") Long ticketId);

    @Update("update ticket_order_item set ticket_id=#{ticketId}, ticket_name=#{ticketName}, unit_price_cent=#{unitPriceCent}, amount_cent=#{amountCent} where id=#{id}")
    int updateTicketForReschedule(@Param("id") Long id,
                                  @Param("ticketId") Long ticketId,
                                  @Param("ticketName") String ticketName,
                                  @Param("unitPriceCent") Integer unitPriceCent,
                                  @Param("amountCent") Integer amountCent);
}
