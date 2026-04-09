package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.RefundRule;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RefundRuleMapper {

    @Select("select * from refund_rule where scenic_id = #{scenicId} order by id asc")
    List<RefundRule> listByScenicId(@Param("scenicId") Long scenicId);

    @Select("select * from refund_rule where id = #{id}")
    RefundRule findById(@Param("id") Long id);

    @Insert("insert into refund_rule(scenic_id, name, free_refund_hours, allow_reschedule) " +
        "values(#{scenicId}, #{name}, #{freeRefundHours}, #{allowReschedule})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RefundRule rule);

    @Update("update refund_rule set name=#{name}, free_refund_hours=#{freeRefundHours}, " +
        "allow_reschedule=#{allowReschedule}, updated_at=now() where id=#{id}")
    int update(RefundRule rule);

    @Delete("delete from refund_rule where id=#{id}")
    int deleteById(@Param("id") Long id);
}
