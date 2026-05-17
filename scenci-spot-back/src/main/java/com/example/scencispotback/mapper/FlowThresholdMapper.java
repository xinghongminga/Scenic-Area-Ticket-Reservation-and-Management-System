package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.FlowThreshold;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
// 阈值映射
public interface FlowThresholdMapper {

    @Insert("insert into flow_threshold(scenic_id, threshold_type, area_code, value, enabled) values(#{scenicId}, #{thresholdType}, #{areaCode}, #{value}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FlowThreshold threshold);

    @Update("update flow_threshold set threshold_type=#{thresholdType}, area_code=#{areaCode}, value=#{value}, enabled=#{enabled}, updated_at=now() where id=#{id}")
    int update(FlowThreshold threshold);

    @Delete("delete from flow_threshold where id=#{id}")
    int delete(@Param("id") Long id);

    @Select("select * from flow_threshold where id=#{id} limit 1")
    FlowThreshold findById(@Param("id") Long id);

    @Select("select * from flow_threshold where scenic_id=#{scenicId} order by id asc")
    List<FlowThreshold> listByScenicId(@Param("scenicId") Long scenicId);

    @Select("select * from flow_threshold where scenic_id=#{scenicId} and enabled=1")
    List<FlowThreshold> listEnabledByScenicId(@Param("scenicId") Long scenicId);
}
