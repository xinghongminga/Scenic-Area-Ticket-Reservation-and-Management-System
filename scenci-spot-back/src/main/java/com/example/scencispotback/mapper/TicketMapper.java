package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.Ticket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 门票映射
public interface TicketMapper {

    List<Ticket> list(@Param("scenicId") Long scenicId,
                      @Param("ticketType") String ticketType,
                      @Param("priceMin") Integer priceMin,
                      @Param("priceMax") Integer priceMax,
                      @Param("keyword") String keyword,
                      @Param("onlyOnline") Boolean onlyOnline);

    Ticket findById(@Param("id") Long id);

    int insert(Ticket ticket);

    int update(Ticket ticket);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") Integer status);

    int deleteById(@Param("id") Long id);
}
