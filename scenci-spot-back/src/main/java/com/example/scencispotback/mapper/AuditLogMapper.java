package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 审计日志映射
public interface AuditLogMapper {

    int insert(AuditLog log);

    List<AuditLog> listAll(@Param("offset") int offset, @Param("pageSize") int pageSize);

    AuditLog findById(@Param("id") Long id);
}
