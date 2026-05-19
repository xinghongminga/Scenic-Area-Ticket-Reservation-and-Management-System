package com.example.scencispotback.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
// SQL moved to resources/mapper/VideoPeopleCountMapper.xml

@Mapper
// 视频人数映射
public interface VideoPeopleCountMapper {

    int insert(@Param("jobId") Long jobId,
               @Param("scenicId") Long scenicId,
               @Param("areaCode") String areaCode,
               @Param("statTime") LocalDateTime statTime,
               @Param("peopleCount") Integer peopleCount);

    int deleteByJobId(@Param("jobId") Long jobId);
}
