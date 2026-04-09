package com.example.scencispotback.api.aftersale;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AftersaleDto {

    public record CreateReq(@NotBlank String orderNo,
                            @NotBlank String reqType,
                            String reason,
                            LocalDate targetVisitDate,
                            Long targetTimeslotId) {}

    public record AuditReq(String auditComment) {}

    public record QueryReq(String userPhone, String status) {}

    public record UpdateReq(String reason,
                            LocalDate targetVisitDate,
                            Long targetTimeslotId) {}

    public record ReqResp(String reqNo,
                          String orderNo,
                          String userNickname,
                          String userPhone,
                          String reqType,
                          String status,
                          String reason,
                          LocalDate targetVisitDate,
                          Long targetTimeslotId,
                          LocalDateTime createdAt) {}

    public record RescheduleTimeslotResp(Long timeslotId,
                                         String timeslotName,
                                         Integer remainQty) {}

    public record RescheduleOptionResp(LocalDate date,
                                       List<RescheduleTimeslotResp> timeslots) {}
}
