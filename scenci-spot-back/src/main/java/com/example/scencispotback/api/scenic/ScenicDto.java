package com.example.scencispotback.api.scenic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

// 景区 DTO
public class ScenicDto {

    public record ScenicResp(Long id, String name, String address, String openTimeDesc,
                              String contactPhone, Integer status) {}

    public record ScenicUpsertReq(@NotBlank String name,
                                   String address,
                                   String openTimeDesc,
                                   String contactPhone) {}

    public record ScenicStatusReq(@NotNull Integer status) {}

    // Timeslot DTOs
    public record TimeslotResp(Long id, Long scenicId, String name,
                                LocalTime startTime, LocalTime endTime, Integer status) {}

    public record TimeslotUpsertReq(@NotNull Long scenicId,
                                     @NotBlank String name,
                                     @NotNull LocalTime startTime,
                                     @NotNull LocalTime endTime) {}

    // RefundRule DTOs
    public record RefundRuleResp(Long id, Long scenicId, String name,
                                  Integer freeRefundHours, Integer allowReschedule) {}

    public record RefundRuleUpsertReq(@NotNull Long scenicId,
                                       @NotBlank String name,
                                       @NotNull Integer freeRefundHours,
                                       @NotNull Integer allowReschedule) {}
}
