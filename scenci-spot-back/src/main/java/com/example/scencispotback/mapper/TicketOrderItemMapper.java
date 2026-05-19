package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.TicketOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 订单项映射
public interface TicketOrderItemMapper {

    int insert(TicketOrderItem item);

    List<TicketOrderItem> listByOrderId(@Param("orderId") Long orderId);

    int countByTicketId(@Param("ticketId") Long ticketId);

    int updateTicketForReschedule(@Param("id") Long id,
                                  @Param("ticketId") Long ticketId,
                                  @Param("ticketName") String ticketName,
                                  @Param("unitPriceCent") Integer unitPriceCent,
                                  @Param("amountCent") Integer amountCent);
}
