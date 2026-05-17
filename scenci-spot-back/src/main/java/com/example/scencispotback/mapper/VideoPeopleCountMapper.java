package com.example.scencispotback.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;

import java.time.LocalDateTime;

@Mapper
// 视频人数映射
public interface VideoPeopleCountMapper {

    @Insert("insert into video_people_count(job_id, scenic_id, area_code, stat_time, people_count) values(#{jobId}, #{scenicId}, #{areaCode}, #{statTime}, #{peopleCount})")
    int insert(@Param("jobId") Long jobId,
               @Param("scenicId") Long scenicId,
               @Param("areaCode") String areaCode,
               @Param("statTime") LocalDateTime statTime,
               @Param("peopleCount") Integer peopleCount);

    @Delete("delete from video_people_count where job_id=#{jobId}")
    int deleteByJobId(@Param("jobId") Long jobId);
}
