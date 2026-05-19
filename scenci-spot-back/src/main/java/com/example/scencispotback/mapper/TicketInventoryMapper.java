package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.TicketInventoryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
// 库存映射
public interface TicketInventoryMapper {

    List<TicketInventoryRow> listByTicketAndDate(@Param("ticketId") Long ticketId, @Param("visitDate") LocalDate visitDate);

    List<TicketInventoryRow> listLatestByTicket(@Param("ticketId") Long ticketId);

    List<TicketInventoryRow> listAvailableByTicketFromDate(@Param("ticketId") Long ticketId, @Param("startDate") LocalDate startDate);

    List<TicketInventoryRow> listAvailableByPriceFromDate(@Param("scenicId") Long scenicId,
                                                          @Param("priceCent") Integer priceCent,
                                                          @Param("startDate") LocalDate startDate);

    TicketInventoryRow lockOne(@Param("ticketId") Long ticketId,
                               @Param("visitDate") LocalDate visitDate,
                               @Param("timeslotId") Long timeslotId);

    TicketInventoryRow findOne(@Param("ticketId") Long ticketId,
                               @Param("visitDate") LocalDate visitDate,
                               @Param("timeslotId") Long timeslotId);

    int create(@Param("ticketId") Long ticketId,
               @Param("visitDate") LocalDate visitDate,
               @Param("timeslotId") Long timeslotId,
               @Param("totalQty") Integer totalQty);

    int adjustTotal(@Param("id") Long id, @Param("delta") Integer delta);

    int addLocked(@Param("id") Long id, @Param("qty") Integer qty);

    int tryAddLocked(@Param("ticketId") Long ticketId,
                     @Param("visitDate") LocalDate visitDate,
                     @Param("timeslotId") Long timeslotId,
                     @Param("qty") Integer qty);

    int subLocked(@Param("id") Long id, @Param("qty") Integer qty);

    int lockToSold(@Param("id") Long id, @Param("qty") Integer qty);

    int tryLockToSold(@Param("ticketId") Long ticketId,
                      @Param("visitDate") LocalDate visitDate,
                      @Param("timeslotId") Long timeslotId,
                      @Param("qty") Integer qty);

    int trySubLocked(@Param("ticketId") Long ticketId,
                     @Param("visitDate") LocalDate visitDate,
                     @Param("timeslotId") Long timeslotId,
                     @Param("qty") Integer qty);

    int addSold(@Param("id") Long id, @Param("qty") Integer qty);

    int subSold(@Param("id") Long id, @Param("qty") Integer qty);

    int deleteByTicketId(@Param("ticketId") Long ticketId);
}
