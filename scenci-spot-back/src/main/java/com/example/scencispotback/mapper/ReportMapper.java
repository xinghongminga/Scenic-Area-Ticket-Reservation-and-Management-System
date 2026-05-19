package com.example.scencispotback.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
// 报表映射
public interface ReportMapper {

    Integer salesAmount(@Param("scenicId") Long scenicId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

    Integer paidOrderCount(@Param("scenicId") Long scenicId,
                           @Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);

    List<Map<String, Object>> salesByTicket(@Param("scenicId") Long scenicId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);
}
