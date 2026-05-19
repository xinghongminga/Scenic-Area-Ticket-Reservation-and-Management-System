package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.VideoAnalysisJob;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.time.LocalDateTime;

@Mapper
// 视频分析映射
public interface VideoAnalysisJobMapper {

    // Insert SQL moved to resources/mapper/VideoAnalysisJobMapper.xml
    int insert(VideoAnalysisJob job);

    VideoAnalysisJob findById(@Param("id") Long id);

    List<VideoAnalysisJob> listAll();

    int deleteById(@Param("id") Long id);

    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("errorMsg") String errorMsg);

    int claimForRunning(@Param("id") Long id);

    int claimForRunningOrStale(@Param("id") Long id, @Param("staleBefore") LocalDateTime staleBefore);
}
