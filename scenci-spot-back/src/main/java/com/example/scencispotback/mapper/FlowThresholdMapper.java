package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.FlowThreshold;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 阈值映射
public interface FlowThresholdMapper {

    int insert(FlowThreshold threshold);

    int update(FlowThreshold threshold);

    int delete(@Param("id") Long id);

    FlowThreshold findById(@Param("id") Long id);

    List<FlowThreshold> listByScenicId(@Param("scenicId") Long scenicId);

    List<FlowThreshold> listEnabledByScenicId(@Param("scenicId") Long scenicId);
}
