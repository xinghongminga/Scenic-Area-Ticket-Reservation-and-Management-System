package com.example.scencispotback.api.flow;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

// 客流 DTO
public class FlowDto {

    public record ThresholdUpsertReq(@NotNull Long scenicId,
                                     @NotBlank String thresholdType,
                                     String areaCode,
                                     @NotNull @Min(1) Integer value,
                                     @NotNull Integer enabled) {}

    public record ThresholdResp(Long id,
                                Long scenicId,
                                String thresholdType,
                                String areaCode,
                                Integer value,
                                Integer enabled) {}

    public record MinutePoint(LocalDateTime statMinute, Integer inCount, Integer outCount, Integer inParkCount) {}

    public record WarningResp(String thresholdType, Integer thresholdValue, Integer currentValue, String message) {}

    public record DailyPoint(String statDate, Integer inCount, Integer outCount) {}

    public record DashboardResp(Long scenicId,
                                Integer currentInPark,
                                Integer todayInTotal,
                                Integer thisMonthInTotal,
                                List<MinutePoint> trend,
                                List<DailyPoint> monthlyTrend,
                                List<WarningResp> warnings) {}
}
