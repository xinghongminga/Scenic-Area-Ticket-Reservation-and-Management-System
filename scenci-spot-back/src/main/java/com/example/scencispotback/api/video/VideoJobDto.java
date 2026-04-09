package com.example.scencispotback.api.video;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class VideoJobDto {
    public record CreateReq(@NotNull Long scenicId,
                            @NotBlank String videoPath,
                            String areaCode,
                            @NotBlank String direction,
                            @NotNull @Min(200) Integer sampleMs) {}

    public record JobResp(Long id,
                          Long scenicId,
                          String videoPath,
                          String areaCode,
                          String direction,
                          Integer sampleMs,
                          String status,
                          String errorMsg,
                          LocalDateTime createdAt) {}

    public record RunResp(Long jobId, String status, Integer pointsWritten, Integer minutesAggregated) {}
}
