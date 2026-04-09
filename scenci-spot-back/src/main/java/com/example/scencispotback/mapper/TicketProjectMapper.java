package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.TicketProject;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TicketProjectMapper {

    @Select("select distinct ticket_id from ticket_project where project_id = #{projectId}")
    List<Long> listTicketIdsByProjectId(@Param("projectId") Long projectId);

    @Select("select * from ticket_project where ticket_id = #{ticketId}")
    List<TicketProject> listByTicketId(@Param("ticketId") Long ticketId);

    @Select({"<script>",
        "select * from ticket_project where ticket_id in",
        "<foreach collection='ticketIds' item='ticketId' open='(' separator=',' close=')'>",
        "#{ticketId}",
        "</foreach>",
        "</script>"})
    List<TicketProject> listByTicketIds(@Param("ticketIds") List<Long> ticketIds);

    @Insert("insert into ticket_project(ticket_id, project_id) values(#{ticketId}, #{projectId})")
    int insert(@Param("ticketId") Long ticketId, @Param("projectId") Long projectId);

    @Delete("delete from ticket_project where ticket_id = #{ticketId}")
    int deleteByTicketId(@Param("ticketId") Long ticketId);
}
