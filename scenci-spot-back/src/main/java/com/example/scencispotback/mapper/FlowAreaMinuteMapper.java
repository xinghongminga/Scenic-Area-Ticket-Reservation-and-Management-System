package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.FlowAreaMinutePoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
// 分区分钟映射
public interface FlowAreaMinuteMapper {

    int upsert(@Param("scenicId") Long scenicId,
               @Param("areaCode") String areaCode,
               @Param("statMinute") LocalDateTime statMinute,
               @Param("crowdCount") Integer crowdCount);

    List<FlowAreaMinutePoint> listRange(@Param("scenicId") Long scenicId,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);
}
