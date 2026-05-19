package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.ScenicArea;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 景区映射
public interface ScenicAreaMapper {

    List<ScenicArea> listAll();

    List<ScenicArea> listAllAdmin();

    ScenicArea findById(@Param("id") Long id);

    List<ScenicArea> listByIds(@Param("ids") List<Long> ids);

    int insert(ScenicArea scenic);

    int update(ScenicArea scenic);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
