package com.example.scencispotback.domain;

import java.time.LocalDate;

public class TicketInventoryRow {
    private Long id;
    private Long ticketId;
    private String ticketName;
    private Integer ticketPriceCent;
    private Integer ticketMorningEnabled;
    private Integer ticketAfternoonEnabled;
    private LocalDate ticketValidDate;
    private LocalDate visitDate;
    private Long timeslotId;
    private String timeslotName;
    private Integer totalQty;
    private Integer soldQty;
    private Integer lockedQty;
    private Integer status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getTicketPriceCent() {
        return ticketPriceCent;
    }

    public void setTicketPriceCent(Integer ticketPriceCent) {
        this.ticketPriceCent = ticketPriceCent;
    }

    public Integer getTicketMorningEnabled() {
        return ticketMorningEnabled;
    }

    public void setTicketMorningEnabled(Integer ticketMorningEnabled) {
        this.ticketMorningEnabled = ticketMorningEnabled;
    }

    public Integer getTicketAfternoonEnabled() {
        return ticketAfternoonEnabled;
    }

    public void setTicketAfternoonEnabled(Integer ticketAfternoonEnabled) {
        this.ticketAfternoonEnabled = ticketAfternoonEnabled;
    }

    public LocalDate getTicketValidDate() {
        return ticketValidDate;
    }

    public void setTicketValidDate(LocalDate ticketValidDate) {
        this.ticketValidDate = ticketValidDate;
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

    public String getTimeslotName() {
        return timeslotName;
    }

    public void setTimeslotName(String timeslotName) {
        this.timeslotName = timeslotName;
    }

    public Integer getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(Integer totalQty) {
        this.totalQty = totalQty;
    }

    public Integer getSoldQty() {
        return soldQty;
    }

    public void setSoldQty(Integer soldQty) {
        this.soldQty = soldQty;
    }

    public Integer getLockedQty() {
        return lockedQty;
    }

    public void setLockedQty(Integer lockedQty) {
        this.lockedQty = lockedQty;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
