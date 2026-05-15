package com.example.scencispotback.domain;

import java.time.LocalDate;

public class Ticket {
    private Long id;
    private Long scenicId;
    private String name;
    private String imageUrl;
    private String ticketType;
    private Integer priceCent;
    private LocalDate validDate;
    private Long refundRuleId;
    private Integer status;
    private Integer stockQty;
    private Integer morningEnabled;
    private Integer afternoonEnabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScenicId() {
        return scenicId;
    }

    public void setScenicId(Long scenicId) {
        this.scenicId = scenicId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTicketType() {
        return ticketType;
    }

    public void setTicketType(String ticketType) {
        this.ticketType = ticketType;
    }

    public Integer getPriceCent() {
        return priceCent;
    }

    public void setPriceCent(Integer priceCent) {
        this.priceCent = priceCent;
    }

    public LocalDate getValidDate() {
        return validDate;
    }

    public void setValidDate(LocalDate validDate) {
        this.validDate = validDate;
    }

    public Long getRefundRuleId() {
        return refundRuleId;
    }

    public void setRefundRuleId(Long refundRuleId) {
        this.refundRuleId = refundRuleId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getStockQty() {
        return stockQty;
    }

    public void setStockQty(Integer stockQty) {
        this.stockQty = stockQty;
    }

    public Integer getMorningEnabled() {
        return morningEnabled;
    }

    public void setMorningEnabled(Integer morningEnabled) {
        this.morningEnabled = morningEnabled;
    }

    public Integer getAfternoonEnabled() {
        return afternoonEnabled;
    }

    public void setAfternoonEnabled(Integer afternoonEnabled) {
        this.afternoonEnabled = afternoonEnabled;
    }

    private String description;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
