package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.TicketOrder;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.time.LocalDateTime;

@Mapper
// 订单映射
public interface TicketOrderMapper {

    @Insert("insert into ticket_order(order_no, scenic_id, user_id, visit_date, timeslot_id, total_amount_cent, status) " +
        "values(#{orderNo}, #{scenicId}, #{userId}, #{visitDate}, #{timeslotId}, #{totalAmountCent}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TicketOrder order);

    @Select("select * from ticket_order where order_no = #{orderNo} limit 1")
    TicketOrder findByOrderNo(@Param("orderNo") String orderNo);

    @Select("select * from ticket_order where order_no = #{orderNo} limit 1 for update")
    TicketOrder findByOrderNoForUpdate(@Param("orderNo") String orderNo);

    @Select("select * from ticket_order where order_no = #{orderNo} and user_id=#{userId} limit 1")
    TicketOrder findByOrderNoAndUserId(@Param("orderNo") String orderNo, @Param("userId") Long userId);

    @Select("select * from ticket_order where id=#{id} limit 1")
    TicketOrder findById(@Param("id") Long id);

    @Select("select * from ticket_order where id=#{id} limit 1 for update")
    TicketOrder findByIdForUpdate(@Param("id") Long id);

    @Select("select id from ticket_order where status='UNPAID' and created_at <= #{cutoff} order by id asc")
    List<Long> findExpiredUnpaidIds(@Param("cutoff") LocalDateTime cutoff);

    @Select("select o.*, i.ticket_name as ticket_name, t.image_url as ticket_image_url " +
        "from ticket_order o " +
        "left join ticket_order_item i on i.id = (select min(i2.id) from ticket_order_item i2 where i2.order_id = o.id) " +
        "left join ticket t on t.id = i.ticket_id " +
        "where o.user_id=#{userId} and o.status != 'DELETED' order by o.id desc")
    List<TicketOrder> findMyOrders(@Param("userId") Long userId);

    @Select({"<script>",
        "select o.*, i.ticket_name as ticket_name, t.image_url as ticket_image_url,",
        "u.phone as user_phone, u.nickname as user_nickname, u.full_name as user_full_name, u.id_card_no as user_id_card_no",
        "from ticket_order o",
        "left join ticket_order_item i on i.id = (select min(i2.id) from ticket_order_item i2 where i2.order_id = o.id)",
        "left join ticket t on t.id = i.ticket_id",
        "left join user_account u on u.id = o.user_id",
        "where 1=1",
        "<if test='status != null and status != \"\"'> and o.status = #{status}</if>",
        "<if test='status == null or status == \"\"'> and o.status != 'DELETED'</if>",
        "<if test='userPhone != null and userPhone != \"\"'> and u.phone like concat('%', #{userPhone}, '%')</if>",
        "<if test='visitDate != null'> and o.visit_date = #{visitDate}</if>",
        "<if test='createdAfter != null'> and o.created_at &gt;= #{createdAfter}</if>",
        "<if test='createdBefore != null'> and o.created_at &lt;= #{createdBefore}</if>",
        "order by o.id desc",
        "</script>"})
    List<TicketOrder> findAll(@Param("status") String status,
                              @Param("userPhone") String userPhone,
                              @Param("visitDate") java.time.LocalDate visitDate,
                              @Param("createdAfter") java.time.LocalDateTime createdAfter,
                              @Param("createdBefore") java.time.LocalDateTime createdBefore);

    @Update("update ticket_order set status=#{toStatus}, updated_at=now() where id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("toStatus") String toStatus);

    @Update("update ticket_order set status=#{toStatus}, close_reason=#{closeReason}, updated_at=now() where id=#{id}")
    int updateStatusAndReason(@Param("id") Long id,
                              @Param("toStatus") String toStatus,
                              @Param("closeReason") String closeReason);

    @Update("update ticket_order set visit_date=#{visitDate}, timeslot_id=#{timeslotId}, updated_at=now() where id=#{id}")
    int updateVisit(@Param("id") Long id,
                    @Param("visitDate") java.time.LocalDate visitDate,
                    @Param("timeslotId") Long timeslotId);
}
