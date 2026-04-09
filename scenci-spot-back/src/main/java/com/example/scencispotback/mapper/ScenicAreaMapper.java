package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.ScenicArea;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ScenicAreaMapper {

    @Select("select * from scenic_area where status = 1 order by id asc")
    List<ScenicArea> listAll();

    @Select("select * from scenic_area order by id asc")
    List<ScenicArea> listAllAdmin();

    @Select("select * from scenic_area where id = #{id}")
    ScenicArea findById(@Param("id") Long id);

    @Select({"<script>",
        "select * from scenic_area where id in",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
        "#{id}",
        "</foreach>",
        "</script>"})
    List<ScenicArea> listByIds(@Param("ids") List<Long> ids);

    @Insert("insert into scenic_area(name, address, open_time_desc, contact_phone, status) " +
        "values(#{name}, #{address}, #{openTimeDesc}, #{contactPhone}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ScenicArea scenic);

    @Update("update scenic_area set name=#{name}, address=#{address}, open_time_desc=#{openTimeDesc}, " +
        "contact_phone=#{contactPhone}, updated_at=now() where id=#{id}")
    int update(ScenicArea scenic);

    @Update("update scenic_area set status=#{status}, updated_at=now() where id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
