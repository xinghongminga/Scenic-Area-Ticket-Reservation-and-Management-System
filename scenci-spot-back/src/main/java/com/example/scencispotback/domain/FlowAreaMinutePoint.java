package com.example.scencispotback.domain;

import java.time.LocalDateTime;

// 分区分钟客流点
public class FlowAreaMinutePoint {
    private Long scenicId;
    private String areaCode;
    private LocalDateTime statMinute;
    private Integer crowdCount;

    public Long getScenicId() { return scenicId; }
    public void setScenicId(Long scenicId) { this.scenicId = scenicId; }
    public String getAreaCode() { return areaCode; }
    public void setAreaCode(String areaCode) { this.areaCode = areaCode; }
    public LocalDateTime getStatMinute() { return statMinute; }
    public void setStatMinute(LocalDateTime statMinute) { this.statMinute = statMinute; }
    public Integer getCrowdCount() { return crowdCount; }
    public void setCrowdCount(Integer crowdCount) { this.crowdCount = crowdCount; }
}
