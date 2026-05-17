package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.FlowAreaMinutePoint;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
// 分区分钟映射
public interface FlowAreaMinuteMapper {

    @Insert("insert into flow_area_minute(scenic_id, area_code, stat_minute, crowd_count) values(#{scenicId}, #{areaCode}, #{statMinute}, #{crowdCount}) on duplicate key update crowd_count=#{crowdCount}, updated_at=now()")
    int upsert(@Param("scenicId") Long scenicId,
               @Param("areaCode") String areaCode,
               @Param("statMinute") LocalDateTime statMinute,
               @Param("crowdCount") Integer crowdCount);

    @Select("select scenic_id, area_code, stat_minute, crowd_count from flow_area_minute where scenic_id=#{scenicId} and stat_minute between #{start} and #{end} order by stat_minute asc")
    List<FlowAreaMinutePoint> listRange(@Param("scenicId") Long scenicId,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);
}
