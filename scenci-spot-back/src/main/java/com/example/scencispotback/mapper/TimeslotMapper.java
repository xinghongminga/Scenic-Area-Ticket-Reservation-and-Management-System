package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.Timeslot;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
// 时间段映射
public interface TimeslotMapper {

    @Select("select * from timeslot where scenic_id = #{scenicId} order by start_time asc")
    List<Timeslot> listByScenicId(@Param("scenicId") Long scenicId);

    @Select("select * from timeslot where scenic_id = #{scenicId} and status = 1 order by start_time asc")
    List<Timeslot> listActiveByScenicId(@Param("scenicId") Long scenicId);

    @Select("select * from timeslot where id = #{id}")
    Timeslot findById(@Param("id") Long id);

    @Insert("insert into timeslot(scenic_id, name, start_time, end_time, status) " +
        "values(#{scenicId}, #{name}, #{startTime}, #{endTime}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Timeslot timeslot);

    @Update("update timeslot set name=#{name}, start_time=#{startTime}, end_time=#{endTime}, " +
        "updated_at=now() where id=#{id}")
    int update(Timeslot timeslot);

    @Update("update timeslot set status=#{status}, updated_at=now() where id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Delete("delete from timeslot where id=#{id}")
    int deleteById(@Param("id") Long id);
}
