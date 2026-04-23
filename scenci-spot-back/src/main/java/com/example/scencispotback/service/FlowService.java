package com.example.scencispotback.service;

import com.example.scencispotback.api.flow.FlowDto;
import com.example.scencispotback.common.BizException;
import com.example.scencispotback.domain.FlowMinutePoint;
import com.example.scencispotback.domain.FlowThreshold;
import com.example.scencispotback.domain.UserAccount;
import com.example.scencispotback.mapper.FlowMinuteMapper;
import com.example.scencispotback.mapper.FlowThresholdMapper;
import com.example.scencispotback.mapper.UserAccountMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FlowService {

    private static final Logger log = LoggerFactory.getLogger(FlowService.class);
    private static final Duration WARNING_NOTIFY_COOLDOWN = Duration.ofMinutes(10);
    private static final Duration WARNING_ACTIVE_TTL = Duration.ofDays(3);

    private final FlowThresholdMapper flowThresholdMapper;
    private final FlowMinuteMapper flowMinuteMapper;
    private final NotificationService notificationService;
    private final UserAccountMapper userAccountMapper;
    private final StringRedisTemplate redisTemplate;

    public FlowService(FlowThresholdMapper flowThresholdMapper,
                       FlowMinuteMapper flowMinuteMapper,
                       NotificationService notificationService,
                       UserAccountMapper userAccountMapper,
                       StringRedisTemplate redisTemplate) {
        this.flowThresholdMapper = flowThresholdMapper;
        this.flowMinuteMapper = flowMinuteMapper;
        this.notificationService = notificationService;
        this.userAccountMapper = userAccountMapper;
        this.redisTemplate = redisTemplate;
    }

    public Long createThreshold(FlowDto.ThresholdUpsertReq req) {
        FlowThreshold threshold = new FlowThreshold();
        threshold.setScenicId(req.scenicId());
        threshold.setThresholdType(req.thresholdType());
        threshold.setAreaCode(req.areaCode());
        threshold.setValue(req.value());
        threshold.setEnabled(req.enabled());
        flowThresholdMapper.insert(threshold);
        return threshold.getId();
    }

    public void updateThreshold(Long id, FlowDto.ThresholdUpsertReq req) {
        FlowThreshold old = flowThresholdMapper.findById(id);
        if (old == null) {
            throw new BizException("阈值配置不存在");
        }
        old.setThresholdType(req.thresholdType());
        old.setAreaCode(req.areaCode());
        old.setValue(req.value());
        old.setEnabled(req.enabled());
        flowThresholdMapper.update(old);
    }

    public void deleteThreshold(Long id) {
        FlowThreshold old = flowThresholdMapper.findById(id);
        if (old == null) {
            throw new BizException("阈值配置不存在");
        }
        flowThresholdMapper.delete(id);
    }

    public List<FlowDto.ThresholdResp> listThreshold(Long scenicId) {
        return flowThresholdMapper.listByScenicId(scenicId).stream().map(t ->
            new FlowDto.ThresholdResp(t.getId(), t.getScenicId(), t.getThresholdType(), t.getAreaCode(), t.getValue(), t.getEnabled())
        ).toList();
    }

    public FlowDto.DashboardResp dashboard(Long scenicId, Integer minutes) {
        int rangeMinutes = (minutes == null || minutes < 5) ? 60 : minutes;
        LocalDateTime end = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime start = end.minusMinutes(rangeMinutes - 1L);

        List<FlowMinutePoint> trendRaw = flowMinuteMapper.listRange(scenicId, start, end);
        int inBeforeRange = flowMinuteMapper.sumInCountBefore(scenicId, start);
        int outBeforeRange = flowMinuteMapper.sumOutCountBefore(scenicId, start);
        int inParkRunning = Math.max(0, inBeforeRange - outBeforeRange);

        List<FlowDto.MinutePoint> trend = new ArrayList<>(trendRaw.size());
        for (FlowMinutePoint p : trendRaw) {
            int in = p.getInCount() == null ? 0 : p.getInCount();
            int out = p.getOutCount() == null ? 0 : p.getOutCount();
            inParkRunning = Math.max(0, inParkRunning + in - out);
            trend.add(new FlowDto.MinutePoint(p.getStatMinute(), in, out, inParkRunning));
        }

        int totalIn = flowMinuteMapper.sumInCountAll(scenicId);
        int totalOut = flowMinuteMapper.sumOutCountAll(scenicId);
        int currentInPark = Math.max(0, totalIn - totalOut);

        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        int todayInTotal = flowMinuteMapper.sumInCount(scenicId, dayStart, end);

        // Monthly daily aggregation
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        List<Map<String, Object>> rawMonthly = flowMinuteMapper.sumByDay(scenicId, monthStart, end);
        List<FlowDto.DailyPoint> monthlyTrend = rawMonthly.stream()
            .map(m -> new FlowDto.DailyPoint(
                (String) m.get("stat_date"),
                ((Number) m.get("in_count")).intValue(),
                ((Number) m.get("out_count")).intValue()
            )).toList();
        int thisMonthInTotal = monthlyTrend.stream().mapToInt(FlowDto.DailyPoint::inCount).sum();

        List<FlowDto.WarningResp> warnings = new ArrayList<>();
        for (FlowThreshold t : flowThresholdMapper.listEnabledByScenicId(scenicId)) {
            if ("INSTANT_MAX".equals(t.getThresholdType()) && currentInPark >= t.getValue()) {
                warnings.add(new FlowDto.WarningResp("INSTANT_MAX", t.getValue(), currentInPark, "当前在园人数超过瞬时阈值"));
            }
            if ("DAILY_MAX".equals(t.getThresholdType()) && todayInTotal >= t.getValue()) {
                warnings.add(new FlowDto.WarningResp("DAILY_MAX", t.getValue(), todayInTotal, "今日累计入园人数超过日阈值"));
            }
        }

        notifyWarningsIfNeeded(scenicId, warnings);

        return new FlowDto.DashboardResp(scenicId, currentInPark, todayInTotal, thisMonthInTotal, trend, monthlyTrend, warnings);
    }

    private void notifyWarningsIfNeeded(Long scenicId, List<FlowDto.WarningResp> warnings) {
        List<UserAccount> receivers = userAccountMapper.listActiveByRoles(List.of("ADMIN", "ANALYST"));
        if (receivers.isEmpty()) {
            return;
        }

        Set<String> activeWarningTypes = new HashSet<>();
        if (warnings != null) {
            warnings.forEach(w -> activeWarningTypes.add(w.thresholdType()));
        }

        for (FlowDto.WarningResp warning : warnings) {
            activeWarningTypes.add(warning.thresholdType());
            String cacheKey = "flow:warning:notify:" + scenicId + ":" + warning.thresholdType();
            Boolean firstHit = redisTemplate.opsForValue().setIfAbsent(cacheKey, String.valueOf(System.currentTimeMillis()), WARNING_NOTIFY_COOLDOWN);
            if (!Boolean.TRUE.equals(firstHit)) {
                continue;
            }

            String activeKey = "flow:warning:active:" + scenicId + ":" + warning.thresholdType();
            redisTemplate.opsForValue().set(activeKey, "1", WARNING_ACTIVE_TTL);

            String title = "客流预警提醒";
            String content = String.format("景区[%d]触发%s预警：%s（阈值=%d，当前=%d）",
                scenicId,
                thresholdTypeText(warning.thresholdType()),
                warning.message(),
                warning.thresholdValue(),
                warning.currentValue());

            for (UserAccount receiver : receivers) {
                try {
                    notificationService.notifySystemMessage(receiver.getId(), title, content);
                } catch (Exception ex) {
                    log.warn("send flow warning notification failed, receiverId={}, scenicId={}, type={}",
                        receiver.getId(), scenicId, warning.thresholdType(), ex);
                }
            }
        }

        Set<String> enabledTypes = new HashSet<>();
        for (FlowThreshold threshold : flowThresholdMapper.listEnabledByScenicId(scenicId)) {
            if ("INSTANT_MAX".equals(threshold.getThresholdType()) || "DAILY_MAX".equals(threshold.getThresholdType())) {
                enabledTypes.add(threshold.getThresholdType());
            }
        }

        for (String thresholdType : enabledTypes) {
            if (activeWarningTypes.contains(thresholdType)) {
                continue;
            }
            String activeKey = "flow:warning:active:" + scenicId + ":" + thresholdType;
            Boolean activeBefore = redisTemplate.hasKey(activeKey);
            if (!Boolean.TRUE.equals(activeBefore)) {
                continue;
            }

            redisTemplate.delete(activeKey);
            String title = "客流预警恢复";
            String content = String.format("景区[%d]的%s预警已恢复，当前已回落到安全范围。", scenicId, thresholdTypeText(thresholdType));
            for (UserAccount receiver : receivers) {
                try {
                    notificationService.notifySystemMessage(receiver.getId(), title, content);
                } catch (Exception ex) {
                    log.warn("send flow recovery notification failed, receiverId={}, scenicId={}, type={}",
                        receiver.getId(), scenicId, thresholdType, ex);
                }
            }
        }
    }

    private String thresholdTypeText(String thresholdType) {
        return switch (thresholdType) {
            case "INSTANT_MAX" -> "瞬时客流阈值";
            case "DAILY_MAX" -> "日累计客流阈值";
            default -> thresholdType;
        };
    }
}
