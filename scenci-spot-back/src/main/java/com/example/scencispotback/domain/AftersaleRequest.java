package com.example.scencispotback.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 售后请求实体
public class AftersaleRequest {
    private Long id;
    private String reqNo;
    private Long orderId;
    private Long userId;
    private String reqType;
    private String reason;
    private String status;
    private Long auditorId;
    private String auditComment;
    private LocalDate targetVisitDate;
    private Long targetTimeslotId;
    private Long targetTicketId;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReqNo() {
        return reqNo;
    }

    public void setReqNo(String reqNo) {
        this.reqNo = reqNo;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getReqType() {
        return reqType;
    }

    public void setReqType(String reqType) {
        this.reqType = reqType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAuditorId() {
        return auditorId;
    }

    public void setAuditorId(Long auditorId) {
        this.auditorId = auditorId;
    }

    public String getAuditComment() {
        return auditComment;
    }

    public void setAuditComment(String auditComment) {
        this.auditComment = auditComment;
    }

    public LocalDate getTargetVisitDate() {
        return targetVisitDate;
    }

    public void setTargetVisitDate(LocalDate targetVisitDate) {
        this.targetVisitDate = targetVisitDate;
    }

    public Long getTargetTimeslotId() {
        return targetTimeslotId;
    }

    public void setTargetTimeslotId(Long targetTimeslotId) {
        this.targetTimeslotId = targetTimeslotId;
    }

    public Long getTargetTicketId() {
        return targetTicketId;
    }

    public void setTargetTicketId(Long targetTicketId) {
        this.targetTicketId = targetTicketId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
