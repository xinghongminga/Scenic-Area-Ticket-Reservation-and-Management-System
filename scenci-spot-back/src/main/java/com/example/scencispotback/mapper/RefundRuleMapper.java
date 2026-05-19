package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.RefundRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 退改规则映射
public interface RefundRuleMapper {

    List<RefundRule> listByScenicId(@Param("scenicId") Long scenicId);

    RefundRule findById(@Param("id") Long id);

    int insert(RefundRule rule);

    int update(RefundRule rule);

    int deleteById(@Param("id") Long id);
}
