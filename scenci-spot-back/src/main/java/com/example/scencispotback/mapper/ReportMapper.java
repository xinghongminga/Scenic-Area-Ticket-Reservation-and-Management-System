package com.example.scencispotback.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
// 报表映射
public interface ReportMapper {

    @Select("select ifnull(sum(total_amount_cent),0) from ticket_order where scenic_id=#{scenicId} and created_at between #{start} and #{end} and status in ('PAID','USED','REFUNDING','RESCHEDULING','RESCHEDULED')")
    Integer salesAmount(@Param("scenicId") Long scenicId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

    @Select("select count(1) from ticket_order where scenic_id=#{scenicId} and created_at between #{start} and #{end} and status in ('PAID','USED','REFUNDING','RESCHEDULING','RESCHEDULED')")
    Integer paidOrderCount(@Param("scenicId") Long scenicId,
                           @Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);

    @Select("select oi.ticket_name as ticketName, ifnull(sum(oi.qty),0) as qty " +
        "from ticket_order_item oi join ticket_order o on oi.order_id=o.id " +
        "where o.scenic_id=#{scenicId} and o.created_at between #{start} and #{end} and o.status in ('PAID','USED','REFUNDING','RESCHEDULING','RESCHEDULED') " +
        "group by oi.ticket_name order by qty desc")
    List<Map<String, Object>> salesByTicket(@Param("scenicId") Long scenicId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);
}
