package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.TicketOrderItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TicketOrderItemMapper {

    @Insert("insert into ticket_order_item(order_id, ticket_id, ticket_name, unit_price_cent, qty, amount_cent) " +
        "values(#{orderId}, #{ticketId}, #{ticketName}, #{unitPriceCent}, #{qty}, #{amountCent})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TicketOrderItem item);

    @Select("select * from ticket_order_item where order_id=#{orderId}")
    List<TicketOrderItem> listByOrderId(@Param("orderId") Long orderId);
}
