package com.example.scencispotback.domain;

import java.time.LocalDateTime;

public class FlowMinutePoint {
    private Long scenicId;
    private LocalDateTime statMinute;
    private Integer inCount;
    private Integer outCount;
    private Integer inParkCount;

    public Long getScenicId() {
        return scenicId;
    }

    public void setScenicId(Long scenicId) {
        this.scenicId = scenicId;
    }

    public LocalDateTime getStatMinute() {
        return statMinute;
    }

    public void setStatMinute(LocalDateTime statMinute) {
        this.statMinute = statMinute;
    }

    public Integer getInCount() {
        return inCount;
    }

    public void setInCount(Integer inCount) {
        this.inCount = inCount;
    }

    public Integer getOutCount() {
        return outCount;
    }

    public void setOutCount(Integer outCount) {
        this.outCount = outCount;
    }

    public Integer getInParkCount() {
        return inParkCount;
    }

    public void setInParkCount(Integer inParkCount) {
        this.inParkCount = inParkCount;
    }
}
