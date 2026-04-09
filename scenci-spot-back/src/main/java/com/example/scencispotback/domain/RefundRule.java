package com.example.scencispotback.domain;

import java.time.LocalDateTime;

public class RefundRule {
    private Long id;
    private Long scenicId;
    private String name;
    private Integer freeRefundHours;
    private Integer allowReschedule;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScenicId() { return scenicId; }
    public void setScenicId(Long scenicId) { this.scenicId = scenicId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getFreeRefundHours() { return freeRefundHours; }
    public void setFreeRefundHours(Integer freeRefundHours) { this.freeRefundHours = freeRefundHours; }
    public Integer getAllowReschedule() { return allowReschedule; }
    public void setAllowReschedule(Integer allowReschedule) { this.allowReschedule = allowReschedule; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
