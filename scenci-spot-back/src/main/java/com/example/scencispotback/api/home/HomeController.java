package com.example.scencispotback.api.home;

import com.example.scencispotback.common.ApiResponse;
import com.example.scencispotback.domain.AftersaleRequest;
import com.example.scencispotback.domain.ScenicArea;
import com.example.scencispotback.domain.TicketOrder;
import com.example.scencispotback.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RestController
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    private final ScenicAreaMapper scenicAreaMapper;
    private final TicketMapper ticketMapper;
    private final TicketOrderMapper ticketOrderMapper;
    private final AftersaleRequestMapper aftersaleRequestMapper;
    private final UserAccountMapper userAccountMapper;
    private final FlowMinuteMapper flowMinuteMapper;

    public HomeController(ScenicAreaMapper scenicAreaMapper,
                          TicketMapper ticketMapper,
                          TicketOrderMapper ticketOrderMapper,
                          AftersaleRequestMapper aftersaleRequestMapper,
                          UserAccountMapper userAccountMapper,
                          FlowMinuteMapper flowMinuteMapper) {
        this.scenicAreaMapper = scenicAreaMapper;
        this.ticketMapper = ticketMapper;
        this.ticketOrderMapper = ticketOrderMapper;
        this.aftersaleRequestMapper = aftersaleRequestMapper;
        this.userAccountMapper = userAccountMapper;
        this.flowMinuteMapper = flowMinuteMapper;
    }

    @GetMapping("/api/home/console")
    /**
     * 后台首页控制台汇总数据。
     */
    public ApiResponse<HomeDto.HomeConsoleResp> console() {
        // ---- 统计数据 ----
        List<ScenicArea> scenics = scenicAreaMapper.listAllAdmin();
        if (scenics == null) {
            scenics = Collections.emptyList();
        }

        LocalDate today = LocalDate.now();

        int totalActiveTickets = 0;
        int totalInPark = 0;
        for (ScenicArea s : scenics) {
            List<com.example.scencispotback.domain.Ticket> tickets = ticketMapper.list(s.getId(), null, null, null, null, true);
            if (tickets != null) {
                totalActiveTickets += (int) tickets.stream()
                    .filter(t -> t.getStatus() != null && t.getStatus() == 1)
                    .filter(t -> t.getValidDate() != null && !t.getValidDate().isBefore(today))
                    .count();
            }
            Integer totalIn = flowMinuteMapper.sumInCountAll(s.getId());
            Integer totalOut = flowMinuteMapper.sumOutCountAll(s.getId());
            totalInPark += Math.max(0, (totalIn == null ? 0 : totalIn) - (totalOut == null ? 0 : totalOut));
        }

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<TicketOrder> todayOrders = ticketOrderMapper.findAll(null, null, null, todayStart, null);
        int todayOrderCount = todayOrders == null ? 0 : todayOrders.size();

        List<AftersaleRequest> pendingAftersales = aftersaleRequestMapper.listAllFiltered(null, "PENDING");
        int pendingAftersaleCount = pendingAftersales == null ? 0 : pendingAftersales.size();

        List<com.example.scencispotback.domain.UserAccount> users = userAccountMapper.listFiltered(null, null, null);
        int totalUserCount = users == null ? 0 : users.size();

        HomeDto.OverviewStats stats = new HomeDto.OverviewStats(
            scenics.size(),
            totalActiveTickets,
            todayOrderCount,
            pendingAftersaleCount,
            totalUserCount,
            totalInPark
        );

        // ---- 最新订单 (前5条) ----
        List<TicketOrder> allOrders = ticketOrderMapper.findAll(null, null, null, null, null);
        if (allOrders == null) {
            allOrders = Collections.emptyList();
        }
        List<HomeDto.OrderItem> latestOrders = allOrders.stream()
            .limit(5)
            .map(o -> new HomeDto.OrderItem(
                o.getId(),
                o.getOrderNo(),
                o.getTicketName(),
                o.getUserNickname() != null ? o.getUserNickname() : o.getUserPhone(),
                o.getStatus(),
                o.getTotalAmountCent(),
                o.getVisitDate(),
                o.getCreatedAt()
            ))
            .toList();

        // ---- 待处理售后 (前5条) ----
        if (pendingAftersales == null) {
            pendingAftersales = Collections.emptyList();
        }
        List<HomeDto.AftersaleItem> aftersaleItems = pendingAftersales.stream()
            .limit(5)
            .map(a -> new HomeDto.AftersaleItem(
                a.getId(),
                a.getReqNo(),
                a.getReqType(),
                a.getReason(),
                a.getStatus(),
                a.getCreatedAt()
            ))
            .toList();

        return ApiResponse.ok(new HomeDto.HomeConsoleResp(stats, latestOrders, aftersaleItems));
    }
}
