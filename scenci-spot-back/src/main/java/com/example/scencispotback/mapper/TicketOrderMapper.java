package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.TicketOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.time.LocalDateTime;

@Mapper
// 订单映射
public interface TicketOrderMapper {

    int insert(TicketOrder order);

    TicketOrder findByOrderNo(@Param("orderNo") String orderNo);

    TicketOrder findByOrderNoForUpdate(@Param("orderNo") String orderNo);

    TicketOrder findByOrderNoAndUserId(@Param("orderNo") String orderNo, @Param("userId") Long userId);

    TicketOrder findById(@Param("id") Long id);

    TicketOrder findByIdForUpdate(@Param("id") Long id);

    List<Long> findExpiredUnpaidIds(@Param("cutoff") LocalDateTime cutoff);

    List<TicketOrder> findMyOrders(@Param("userId") Long userId);

    List<TicketOrder> findAll(@Param("status") String status,
                              @Param("userPhone") String userPhone,
                              @Param("visitDate") java.time.LocalDate visitDate,
                              @Param("createdAfter") java.time.LocalDateTime createdAfter,
                              @Param("createdBefore") java.time.LocalDateTime createdBefore);

    int updateStatus(@Param("id") Long id, @Param("toStatus") String toStatus);

    int updateStatusAndReason(@Param("id") Long id,
                              @Param("toStatus") String toStatus,
                              @Param("closeReason") String closeReason);

    int updateVisit(@Param("id") Long id,
                    @Param("visitDate") java.time.LocalDate visitDate,
                    @Param("timeslotId") Long timeslotId);
}
