package com.example.scencispotback.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TicketOrder {
    private Long id;
    private String orderNo;
    private Long scenicId;
    private Long userId;
    private LocalDate visitDate;
    private Long timeslotId;
    private Integer totalAmountCent;
    private String status;
    private String closeReason;
    private LocalDateTime createdAt;
    private String ticketName;
    private String ticketImageUrl;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getScenicId() {
        return scenicId;
    }

    public void setScenicId(Long scenicId) {
        this.scenicId = scenicId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    public Long getTimeslotId() {
        return timeslotId;
    }

    public void setTimeslotId(Long timeslotId) {
        this.timeslotId = timeslotId;
    }

    public Integer getTotalAmountCent() {
        return totalAmountCent;
    }

    public void setTotalAmountCent(Integer totalAmountCent) {
        this.totalAmountCent = totalAmountCent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTicketName() {
        return ticketName;
    }

    public void setTicketName(String ticketName) {
        this.ticketName = ticketName;
    }

    public String getTicketImageUrl() {
        return ticketImageUrl;
    }

    public void setTicketImageUrl(String ticketImageUrl) {
        this.ticketImageUrl = ticketImageUrl;
    }

    // Joined fields from user_account
    private String userPhone;
    private String userNickname;
    private String userFullName;
    private String userIdCardNo;

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public String getUserNickname() { return userNickname; }
    public void setUserNickname(String userNickname) { this.userNickname = userNickname; }
    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }
    public String getUserIdCardNo() { return userIdCardNo; }
    public void setUserIdCardNo(String userIdCardNo) { this.userIdCardNo = userIdCardNo; }
}
