package com.example.scencispotback.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.example.scencispotback.domain.FlowMinutePoint;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface FlowMinuteMapper {

    @Insert("insert into flow_minute(scenic_id, stat_minute, in_count, out_count, in_park_count) values(#{scenicId}, #{statMinute}, #{inCount}, 0, #{inParkCount}) " +
        "on duplicate key update in_count=in_count+#{inCount}, in_park_count=in_park_count+#{inParkCount}, updated_at=now()")
    int upsertIn(@Param("scenicId") Long scenicId,
                 @Param("statMinute") LocalDateTime statMinute,
                 @Param("inCount") Integer inCount,
                 @Param("inParkCount") Integer inParkCount);

    @Select("select scenic_id, stat_minute, in_count, out_count, in_park_count from flow_minute where scenic_id=#{scenicId} and stat_minute between #{start} and #{end} order by stat_minute asc")
    List<FlowMinutePoint> listRange(@Param("scenicId") Long scenicId,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    @Select("select scenic_id, stat_minute, in_count, out_count, in_park_count from flow_minute where scenic_id=#{scenicId} order by stat_minute desc limit 1")
    FlowMinutePoint latest(@Param("scenicId") Long scenicId);

    @Select("select ifnull(sum(in_count),0) from flow_minute where scenic_id=#{scenicId} and stat_minute between #{start} and #{end}")
    Integer sumInCount(@Param("scenicId") Long scenicId,
                       @Param("start") LocalDateTime start,
                       @Param("end") LocalDateTime end);

    @Select("select ifnull(sum(out_count),0) from flow_minute where scenic_id=#{scenicId} and stat_minute between #{start} and #{end}")
    Integer sumOutCount(@Param("scenicId") Long scenicId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

    @Select("select ifnull(sum(in_count),0) from flow_minute where scenic_id=#{scenicId}")
    Integer sumInCountAll(@Param("scenicId") Long scenicId);

    @Select("select ifnull(sum(out_count),0) from flow_minute where scenic_id=#{scenicId}")
    Integer sumOutCountAll(@Param("scenicId") Long scenicId);

    @Insert("insert into flow_minute(scenic_id, stat_minute, in_count, out_count, in_park_count) values(#{scenicId}, #{statMinute}, 0, 0, #{inParkCount}) " +
        "on duplicate key update in_park_count=#{inParkCount}, updated_at=now()")
    int upsertInParkSnapshot(@Param("scenicId") Long scenicId,
                             @Param("statMinute") LocalDateTime statMinute,
                             @Param("inParkCount") Integer inParkCount);

    @Select("select ifnull(sum(in_count),0) from flow_minute where scenic_id=#{scenicId} and stat_minute < #{before}")
    Integer sumInCountBefore(@Param("scenicId") Long scenicId,
                             @Param("before") LocalDateTime before);

    @Select("select ifnull(sum(out_count),0) from flow_minute where scenic_id=#{scenicId} and stat_minute < #{before}")
    Integer sumOutCountBefore(@Param("scenicId") Long scenicId,
                              @Param("before") LocalDateTime before);

    @Insert("insert into flow_minute(scenic_id, stat_minute, in_count, out_count, in_park_count) values(#{scenicId}, #{statMinute}, 0, #{outCount}, 0) " +
        "on duplicate key update out_count=out_count+#{outCount}, in_park_count=GREATEST(0, in_park_count-#{outCount}), updated_at=now()")
    int upsertOut(@Param("scenicId") Long scenicId,
                  @Param("statMinute") LocalDateTime statMinute,
                  @Param("outCount") Integer outCount);

    @Select("SELECT DATE_FORMAT(stat_minute, '%Y-%m-%d') AS stat_date, " +
        "IFNULL(SUM(in_count),0) AS in_count, IFNULL(SUM(out_count),0) AS out_count " +
        "FROM flow_minute WHERE scenic_id=#{scenicId} AND stat_minute BETWEEN #{start} AND #{end} " +
        "GROUP BY DATE(stat_minute) ORDER BY stat_date ASC")
    List<Map<String, Object>> sumByDay(@Param("scenicId") Long scenicId,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);
}
