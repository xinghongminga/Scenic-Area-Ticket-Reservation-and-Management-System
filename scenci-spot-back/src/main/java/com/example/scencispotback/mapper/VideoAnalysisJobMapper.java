package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.VideoAnalysisJob;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.time.LocalDateTime;

@Mapper
// 视频分析映射
public interface VideoAnalysisJobMapper {

    @Insert("insert into video_analysis_job(scenic_id, video_path, area_code, direction, sample_ms, status, created_by) values(#{scenicId}, #{videoPath}, #{areaCode}, #{direction}, #{sampleMs}, #{status}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(VideoAnalysisJob job);

    @Select("select * from video_analysis_job where id=#{id} limit 1")
    VideoAnalysisJob findById(@Param("id") Long id);

    @Select("select * from video_analysis_job order by id desc")
    List<VideoAnalysisJob> listAll();

    @Delete("delete from video_analysis_job where id=#{id}")
    int deleteById(@Param("id") Long id);

    @Update("update video_analysis_job set status=#{status}, error_msg=#{errorMsg}, updated_at=now() where id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("errorMsg") String errorMsg);

    @Update("update video_analysis_job set status='RUNNING', error_msg=null, updated_at=now() where id=#{id} and status in ('PENDING','FAILED')")
    int claimForRunning(@Param("id") Long id);

    @Update("update video_analysis_job set status='RUNNING', error_msg=null, updated_at=now() where id=#{id} and (status in ('PENDING','FAILED') or (status='RUNNING' and updated_at < #{staleBefore}))")
    int claimForRunningOrStale(@Param("id") Long id, @Param("staleBefore") LocalDateTime staleBefore);
}
