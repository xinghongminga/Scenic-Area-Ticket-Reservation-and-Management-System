package com.example.scencispotback.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.scencispotback.domain.FlowMinutePoint;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
// 分钟流量映射
public interface FlowMinuteMapper {

    int upsertIn(@Param("scenicId") Long scenicId,
                 @Param("statMinute") LocalDateTime statMinute,
                 @Param("inCount") Integer inCount,
                 @Param("inParkCount") Integer inParkCount);

    List<FlowMinutePoint> listRange(@Param("scenicId") Long scenicId,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    FlowMinutePoint latest(@Param("scenicId") Long scenicId);

    Integer sumInCount(@Param("scenicId") Long scenicId,
                       @Param("start") LocalDateTime start,
                       @Param("end") LocalDateTime end);

    Integer sumOutCount(@Param("scenicId") Long scenicId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

    Integer sumInCountAll(@Param("scenicId") Long scenicId);

    Integer sumOutCountAll(@Param("scenicId") Long scenicId);

    int upsertInParkSnapshot(@Param("scenicId") Long scenicId,
                             @Param("statMinute") LocalDateTime statMinute,
                             @Param("inParkCount") Integer inParkCount);

    Integer sumInCountBefore(@Param("scenicId") Long scenicId,
                             @Param("before") LocalDateTime before);

    Integer sumOutCountBefore(@Param("scenicId") Long scenicId,
                              @Param("before") LocalDateTime before);

    int upsertOut(@Param("scenicId") Long scenicId,
                  @Param("statMinute") LocalDateTime statMinute,
                  @Param("outCount") Integer outCount);

    List<Map<String, Object>> sumByDay(@Param("scenicId") Long scenicId,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);
}
