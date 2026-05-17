package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.TicketInventoryRow;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
// 库存映射
public interface TicketInventoryMapper {

    @Select("select i.id, i.ticket_id, i.visit_date, i.timeslot_id, t.name as timeslot_name, i.total_qty, i.sold_qty, i.locked_qty, i.status " +
        "from ticket_inventory i join timeslot t on i.timeslot_id=t.id where i.ticket_id=#{ticketId} and i.visit_date=#{visitDate}")
    List<TicketInventoryRow> listByTicketAndDate(@Param("ticketId") Long ticketId, @Param("visitDate") LocalDate visitDate);

    @Select("select i.id, i.ticket_id, i.visit_date, i.timeslot_id, t.name as timeslot_name, i.total_qty, i.sold_qty, i.locked_qty, i.status " +
        "from ticket_inventory i join timeslot t on i.timeslot_id=t.id " +
        "where i.ticket_id=#{ticketId} and i.visit_date=(" +
        "select max(i2.visit_date) from ticket_inventory i2 where i2.ticket_id=i.ticket_id and i2.timeslot_id=i.timeslot_id)")
    List<TicketInventoryRow> listLatestByTicket(@Param("ticketId") Long ticketId);

    @Select("select i.id, i.ticket_id, i.visit_date, i.timeslot_id, t.name as timeslot_name, i.total_qty, i.sold_qty, i.locked_qty, i.status " +
        "from ticket_inventory i join timeslot t on i.timeslot_id=t.id " +
        "where i.ticket_id=#{ticketId} and i.visit_date>=#{startDate} and i.status=1 and (i.total_qty - i.sold_qty - i.locked_qty) > 0 " +
        "order by i.visit_date asc, i.timeslot_id asc")
    List<TicketInventoryRow> listAvailableByTicketFromDate(@Param("ticketId") Long ticketId, @Param("startDate") LocalDate startDate);

    @Select("select i.id, i.ticket_id, tk.name as ticket_name, tk.price_cent as ticket_price_cent, " +
        "tk.morning_enabled as ticket_morning_enabled, tk.afternoon_enabled as ticket_afternoon_enabled, " +
        "tk.valid_date as ticket_valid_date, i.visit_date, i.timeslot_id, ts.name as timeslot_name, " +
        "i.total_qty, i.sold_qty, i.locked_qty, i.status " +
        "from ticket_inventory i " +
        "join timeslot ts on i.timeslot_id=ts.id " +
        "join ticket tk on i.ticket_id=tk.id " +
        "where tk.scenic_id=#{scenicId} and tk.price_cent=#{priceCent} and tk.status=1 " +
        "and i.visit_date>=#{startDate} and i.status=1 and (i.total_qty - i.sold_qty - i.locked_qty) > 0 " +
        "order by tk.id asc, i.visit_date asc, i.timeslot_id asc")
    List<TicketInventoryRow> listAvailableByPriceFromDate(@Param("scenicId") Long scenicId,
                                                          @Param("priceCent") Integer priceCent,
                                                          @Param("startDate") LocalDate startDate);

    @Select("select * from ticket_inventory where ticket_id=#{ticketId} and visit_date=#{visitDate} and timeslot_id=#{timeslotId} for update")
    TicketInventoryRow lockOne(@Param("ticketId") Long ticketId,
                               @Param("visitDate") LocalDate visitDate,
                               @Param("timeslotId") Long timeslotId);

    @Select("select * from ticket_inventory where ticket_id=#{ticketId} and visit_date=#{visitDate} and timeslot_id=#{timeslotId} limit 1")
    TicketInventoryRow findOne(@Param("ticketId") Long ticketId,
                               @Param("visitDate") LocalDate visitDate,
                               @Param("timeslotId") Long timeslotId);

    @Insert("insert into ticket_inventory(ticket_id, visit_date, timeslot_id, total_qty, sold_qty, locked_qty, status) " +
        "values(#{ticketId}, #{visitDate}, #{timeslotId}, #{totalQty}, 0, 0, 1)")
    int create(@Param("ticketId") Long ticketId,
               @Param("visitDate") LocalDate visitDate,
               @Param("timeslotId") Long timeslotId,
               @Param("totalQty") Integer totalQty);

    @Update("update ticket_inventory set total_qty=GREATEST(0, total_qty + #{delta}), updated_at=now() where id=#{id}")
    int adjustTotal(@Param("id") Long id, @Param("delta") Integer delta);

    @Update("update ticket_inventory set locked_qty=locked_qty + #{qty}, updated_at=now() where id=#{id}")
    int addLocked(@Param("id") Long id, @Param("qty") Integer qty);

    @Update("update ticket_inventory set locked_qty=locked_qty + #{qty}, updated_at=now() " +
        "where ticket_id=#{ticketId} and visit_date=#{visitDate} and timeslot_id=#{timeslotId} " +
        "and status=1 and (total_qty - sold_qty - locked_qty) >= #{qty}")
    int tryAddLocked(@Param("ticketId") Long ticketId,
                     @Param("visitDate") LocalDate visitDate,
                     @Param("timeslotId") Long timeslotId,
                     @Param("qty") Integer qty);

    @Update("update ticket_inventory set locked_qty=GREATEST(0, locked_qty - #{qty}), updated_at=now() where id=#{id}")
    int subLocked(@Param("id") Long id, @Param("qty") Integer qty);

    @Update("update ticket_inventory set locked_qty=locked_qty - #{qty}, sold_qty=sold_qty + #{qty}, updated_at=now() where id=#{id}")
    int lockToSold(@Param("id") Long id, @Param("qty") Integer qty);

    @Update("update ticket_inventory set locked_qty=locked_qty - #{qty}, sold_qty=sold_qty + #{qty}, updated_at=now() " +
        "where ticket_id=#{ticketId} and visit_date=#{visitDate} and timeslot_id=#{timeslotId} and locked_qty >= #{qty}")
    int tryLockToSold(@Param("ticketId") Long ticketId,
                      @Param("visitDate") LocalDate visitDate,
                      @Param("timeslotId") Long timeslotId,
                      @Param("qty") Integer qty);

    @Update("update ticket_inventory set locked_qty=locked_qty - #{qty}, updated_at=now() " +
        "where ticket_id=#{ticketId} and visit_date=#{visitDate} and timeslot_id=#{timeslotId} and locked_qty >= #{qty}")
    int trySubLocked(@Param("ticketId") Long ticketId,
                     @Param("visitDate") LocalDate visitDate,
                     @Param("timeslotId") Long timeslotId,
                     @Param("qty") Integer qty);

    @Update("update ticket_inventory set sold_qty=sold_qty + #{qty}, updated_at=now() where id=#{id}")
    int addSold(@Param("id") Long id, @Param("qty") Integer qty);

    @Update("update ticket_inventory set sold_qty=GREATEST(0, sold_qty - #{qty}), updated_at=now() where id=#{id}")
    int subSold(@Param("id") Long id, @Param("qty") Integer qty);

    @Delete("delete from ticket_inventory where ticket_id = #{ticketId}")
    int deleteByTicketId(@Param("ticketId") Long ticketId);
}
