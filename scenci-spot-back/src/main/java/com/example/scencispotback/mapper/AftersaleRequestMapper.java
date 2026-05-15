package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.AftersaleRequest;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AftersaleRequestMapper {

    @Insert("insert into aftersale_request(req_no, order_id, user_id, req_type, reason, status, target_visit_date, target_timeslot_id, target_ticket_id) " +
        "values(#{reqNo}, #{orderId}, #{userId}, #{reqType}, #{reason}, #{status}, #{targetVisitDate}, #{targetTimeslotId}, #{targetTicketId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AftersaleRequest req);

    @Select("select * from aftersale_request where req_no=#{reqNo} limit 1")
    AftersaleRequest findByReqNo(@Param("reqNo") String reqNo);

    @Select("select * from aftersale_request where req_no=#{reqNo} limit 1 for update")
    AftersaleRequest lockByReqNo(@Param("reqNo") String reqNo);

    @Select("select * from aftersale_request where user_id=#{userId} order by id desc")
    List<AftersaleRequest> listByUserId(@Param("userId") Long userId);

    @Select("select * from aftersale_request order by id desc")
    List<AftersaleRequest> listAll();

    @Select({"<script>",
        "select ar.* from aftersale_request ar",
        "join ticket_order o on o.id = ar.order_id",
        "join user_account u on u.id = o.user_id",
        "where 1=1",
        "<if test='userPhone != null and userPhone != \"\"'> and u.phone like concat('%', #{userPhone}, '%')</if>",
        "<if test='status != null and status != \"\"'> and ar.status = #{status}</if>",
        "order by ar.id desc",
        "</script>"})
    List<AftersaleRequest> listAllFiltered(@Param("userPhone") String userPhone, @Param("status") String status);

    @Update("update aftersale_request set status=#{status}, auditor_id=#{auditorId}, audit_comment=#{auditComment}, updated_at=now() where id=#{id}")
    int updateAudit(@Param("id") Long id,
                    @Param("status") String status,
                    @Param("auditorId") Long auditorId,
                    @Param("auditComment") String auditComment);

    @Update("update aftersale_request set reason=#{reason}, target_visit_date=#{targetVisitDate}, target_timeslot_id=#{targetTimeslotId}, target_ticket_id=#{targetTicketId}, updated_at=now() where id=#{id}")
    int updateContent(@Param("id") Long id,
                      @Param("reason") String reason,
                      @Param("targetVisitDate") java.time.LocalDate targetVisitDate,
                      @Param("targetTimeslotId") Long targetTimeslotId,
                      @Param("targetTicketId") Long targetTicketId);

    @Delete("delete from aftersale_request where id=#{id}")
    int deleteById(@Param("id") Long id);
}
