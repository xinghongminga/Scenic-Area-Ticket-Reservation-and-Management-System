package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.Timeslot;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
// 时间段映射
public interface TimeslotMapper {

    List<Timeslot> listByScenicId(@Param("scenicId") Long scenicId);

    List<Timeslot> listActiveByScenicId(@Param("scenicId") Long scenicId);

    Timeslot findById(@Param("id") Long id);

    // Insert SQL moved to resources/mapper/TimeslotMapper.xml
    int insert(Timeslot timeslot);

    // Update SQL moved to resources/mapper/TimeslotMapper.xml
    int update(Timeslot timeslot);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int deleteById(@Param("id") Long id);
}
