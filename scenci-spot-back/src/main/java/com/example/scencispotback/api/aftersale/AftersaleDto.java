package com.example.scencispotback.api.aftersale;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// 售后 DTO
public class AftersaleDto {

    public record CreateReq(@NotBlank String orderNo,
                            @NotBlank String reqType,
                            String reason,
                            LocalDate targetVisitDate,
                            Long targetTimeslotId,
                            Long targetTicketId) {}

    public record AuditReq(String auditComment) {}

    public record QueryReq(String userPhone, String status) {}

    public record UpdateReq(String reason,
                            LocalDate targetVisitDate,
                            Long targetTimeslotId,
                            Long targetTicketId) {}

    public record ReqResp(String reqNo,
                          String orderNo,
                          String userNickname,
                          String userPhone,
                          String reqType,
                          String status,
                          String reason,
                          LocalDate targetVisitDate,
                          Long targetTimeslotId,
                          Long targetTicketId,
                          LocalDateTime createdAt) {}

    public record RescheduleTimeslotResp(Long timeslotId,
                                         String timeslotName,
                                         Integer remainQty) {}

    public record RescheduleOptionResp(Long ticketId,
                                       String ticketName,
                                       Integer ticketPriceCent,
                                       LocalDate date,
                                       List<RescheduleTimeslotResp> timeslots) {}
}
