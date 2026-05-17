package com.example.scencispotback.domain;

// 订单项实体
public class TicketOrderItem {
    private Long id;
    private Long orderId;
    private Long ticketId;
    private String ticketName;
    private Integer unitPriceCent;
    private Integer qty;
    private Integer amountCent;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketName() {
        return ticketName;
    }

    public void setTicketName(String ticketName) {
        this.ticketName = ticketName;
    }

    public Integer getUnitPriceCent() {
        return unitPriceCent;
    }

    public void setUnitPriceCent(Integer unitPriceCent) {
        this.unitPriceCent = unitPriceCent;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public Integer getAmountCent() {
        return amountCent;
    }

    public void setAmountCent(Integer amountCent) {
        this.amountCent = amountCent;
    }
}
