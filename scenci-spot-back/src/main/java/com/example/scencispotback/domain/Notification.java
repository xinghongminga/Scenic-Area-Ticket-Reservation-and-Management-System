package com.example.scencispotback.domain;

import java.time.LocalDateTime;

public class Notification {
    private Long id;
    private Long receiverId;
    private String title;
    private String content;
    private String ntype;
    private Integer isRead;
    private Long createdBy;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getNtype() { return ntype; }
    public void setNtype(String ntype) { this.ntype = ntype; }
    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
