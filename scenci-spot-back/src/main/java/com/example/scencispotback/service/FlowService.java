package com.example.scencispotback.service;

import com.example.scencispotback.api.flow.FlowDto;
import com.example.scencispotback.common.BizException;
import com.example.scencispotback.domain.FlowMinutePoint;
import com.example.scencispotback.domain.FlowThreshold;
import com.example.scencispotback.mapper.FlowMinuteMapper;
import com.example.scencispotback.mapper.FlowThresholdMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FlowService {

    private final FlowThresholdMapper flowThresholdMapper;
    private final FlowMinuteMapper flowMinuteMapper;

    public FlowService(FlowThresholdMapper flowThresholdMapper, FlowMinuteMapper flowMinuteMapper) {
        this.flowThresholdMapper = flowThresholdMapper;
        this.flowMinuteMapper = flowMinuteMapper;
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

        return new FlowDto.DashboardResp(scenicId, currentInPark, todayInTotal, thisMonthInTotal, trend, monthlyTrend, warnings);
    }
}
