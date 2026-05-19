package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.AftersaleRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 售后映射
public interface AftersaleRequestMapper {

    int insert(AftersaleRequest req);

    AftersaleRequest findByReqNo(@Param("reqNo") String reqNo);

    AftersaleRequest lockByReqNo(@Param("reqNo") String reqNo);

    List<AftersaleRequest> listByUserId(@Param("userId") Long userId);

    List<AftersaleRequest> listAll();

    List<AftersaleRequest> listAllFiltered(@Param("userPhone") String userPhone, @Param("status") String status);

    int updateAudit(@Param("id") Long id,
                    @Param("status") String status,
                    @Param("auditorId") Long auditorId,
                    @Param("auditComment") String auditComment);

    int updateContent(@Param("id") Long id,
                      @Param("reason") String reason,
                      @Param("targetVisitDate") java.time.LocalDate targetVisitDate,
                      @Param("targetTimeslotId") Long targetTimeslotId,
                      @Param("targetTicketId") Long targetTicketId);

    int deleteById(@Param("id") Long id);
}
