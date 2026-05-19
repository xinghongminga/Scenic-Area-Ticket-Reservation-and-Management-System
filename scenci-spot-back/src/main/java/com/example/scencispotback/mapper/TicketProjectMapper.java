package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.TicketProject;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
// 票务项目映射
public interface TicketProjectMapper {

    // List<Long> listTicketIdsByProjectId(@Param("projectId") Long projectId);
    List<Long> listTicketIdsByProjectId(@Param("projectId") Long projectId);

    // List<TicketProject> listByTicketId(@Param("ticketId") Long ticketId);
    List<TicketProject> listByTicketId(@Param("ticketId") Long ticketId);

    // List<TicketProject> listByTicketIds(@Param("ticketIds") List<Long> ticketIds);
    List<TicketProject> listByTicketIds(@Param("ticketIds") List<Long> ticketIds);

    // int insert(@Param("ticketId") Long ticketId, @Param("projectId") Long projectId);
    int insert(@Param("ticketId") Long ticketId, @Param("projectId") Long projectId);

    // int deleteByTicketId(@Param("ticketId") Long ticketId);
    int deleteByTicketId(@Param("ticketId") Long ticketId);
}
