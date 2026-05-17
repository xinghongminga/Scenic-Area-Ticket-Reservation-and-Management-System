package com.example.scencispotback.api.ticket;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

// 门票 DTO
public class TicketDto {

    public record TicketQuery(Long scenicId, String ticketType, Integer priceMin, Integer priceMax, String keyword, LocalDate visitDate) {}

    public record InventoryResp(Long timeslotId, String timeslotName, Integer totalQty, Integer soldQty, Integer lockedQty, Integer remainQty) {}

    public record AdminTicketUpsertReq(@NotNull Long scenicId,
                                       @NotBlank String name,
                                       String imageUrl,
                                       String description,
                                       @NotBlank String ticketType,
                                       @NotNull @Min(1) Integer priceCent,
                                       @NotNull @Min(0) Integer stockQty,
                                       @Min(0) Integer morningStockQty,
                                       @Min(0) Integer afternoonStockQty,
                                       Integer morningEnabled,
                                       Integer afternoonEnabled,
                                       LocalDate validDate,
                                       Long refundRuleId,
                                       List<Long> projectIds) {}

    public record TicketStatusReq(@NotNull Integer status) {}

    public record AdjustInventoryReq(@NotNull Long ticketId,
                                     @NotNull LocalDate visitDate,
                                     @NotNull Long timeslotId,
                                     @NotNull Integer delta) {}

    public record TicketListResp(Long id,
                                 Long scenicId,
                                 String name,
                                 String imageUrl,
                                 String description,
                                 String ticketType,
                                 Integer priceCent,
                                 Integer stockQty,
                                 Integer morningStockQty,
                                 Integer afternoonStockQty,
                                 Integer morningEnabled,
                                 Integer afternoonEnabled,
                                 LocalDate validDate,
                                 Integer status,
                                 List<Long> projectIds,
                                 String projectNames) {}

    public record TicketDetailResp(TicketListResp ticket, List<InventoryResp> inventory) {}
}
