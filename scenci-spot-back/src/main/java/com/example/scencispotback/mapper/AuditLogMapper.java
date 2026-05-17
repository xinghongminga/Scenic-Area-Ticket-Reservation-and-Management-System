package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.AuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
// 审计日志映射
public interface AuditLogMapper {

    @Insert("insert into sys_audit_log(operator_id, operator_role, module, action, target_type, target_id, detail, ip) " +
        "values(#{operatorId}, #{operatorRole}, #{module}, #{action}, #{targetType}, #{targetId}, #{detail}, #{ip})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AuditLog log);

    @Select("select * from sys_audit_log order by id desc limit #{pageSize} offset #{offset}")
    List<AuditLog> listAll(@Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("select * from sys_audit_log where id=#{id} limit 1")
    AuditLog findById(@Param("id") Long id);
}
