package com.example.scencispotback.service;

import com.example.scencispotback.api.scenic.ScenicDto;
import com.example.scencispotback.common.BizException;
import com.example.scencispotback.domain.RefundRule;
import com.example.scencispotback.domain.ScenicArea;
import com.example.scencispotback.domain.Timeslot;
import com.example.scencispotback.mapper.RefundRuleMapper;
import com.example.scencispotback.mapper.ScenicAreaMapper;
import com.example.scencispotback.mapper.TicketMapper;
import com.example.scencispotback.mapper.TicketProjectMapper;
import com.example.scencispotback.mapper.TimeslotMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
// 景区服务
public class ScenicService {

    private final ScenicAreaMapper scenicAreaMapper;
    private final TimeslotMapper timeslotMapper;
    private final RefundRuleMapper refundRuleMapper;
    private final TicketProjectMapper ticketProjectMapper;
    private final TicketMapper ticketMapper;

    public ScenicService(ScenicAreaMapper scenicAreaMapper,
                         TimeslotMapper timeslotMapper,
                         RefundRuleMapper refundRuleMapper,
                         TicketProjectMapper ticketProjectMapper,
                         TicketMapper ticketMapper) {
        this.scenicAreaMapper = scenicAreaMapper;
        this.timeslotMapper = timeslotMapper;
        this.refundRuleMapper = refundRuleMapper;
        this.ticketProjectMapper = ticketProjectMapper;
        this.ticketMapper = ticketMapper;
    }

    // ========== Scenic Area ==========

    /**
     * 查询景区列表（管理员可查看全部）。
     */
    public List<ScenicDto.ScenicResp> listScenics(boolean adminMode) {
        List<ScenicArea> list = adminMode ? scenicAreaMapper.listAllAdmin() : scenicAreaMapper.listAll();
        return list.stream().map(this::toScenicResp).toList();
    }

    /**
     * 查询单个景区详情。
     */
    public ScenicDto.ScenicResp getScenic(Long id) {
        ScenicArea s = scenicAreaMapper.findById(id);
        if (s == null) throw new BizException("景区不存在");
        return toScenicResp(s);
    }

    @Transactional
    /**
     * 创建景区。
     */
    public Long createScenic(ScenicDto.ScenicUpsertReq req) {
        ScenicArea s = new ScenicArea();
        s.setName(req.name());
        s.setAddress(req.address());
        s.setOpenTimeDesc(req.openTimeDesc());
        s.setContactPhone(req.contactPhone());
        s.setStatus(1);
        scenicAreaMapper.insert(s);
        return s.getId();
    }

    @Transactional
    /**
     * 更新景区信息。
     */
    public void updateScenic(Long id, ScenicDto.ScenicUpsertReq req) {
        ScenicArea s = scenicAreaMapper.findById(id);
        if (s == null) throw new BizException("景区不存在");
        s.setName(req.name());
        s.setAddress(req.address());
        s.setOpenTimeDesc(req.openTimeDesc());
        s.setContactPhone(req.contactPhone());
        scenicAreaMapper.update(s);
    }

    @Transactional
    /**
     * 更新景区状态并同步关联门票状态。
     */
    public void updateScenicStatus(Long id, Integer status) {
        if (scenicAreaMapper.updateStatus(id, status) == 0) throw new BizException("景区不存在");
        if (status != null && status == 0) {
            List<Long> relatedTicketIds = ticketProjectMapper.listTicketIdsByProjectId(id);
            if (!relatedTicketIds.isEmpty()) {
                ticketMapper.batchUpdateStatus(relatedTicketIds, 0);
            }
        }
    }

    // ========== Timeslot ==========

    /**
     * 查询景区时段列表。
     */
    public List<ScenicDto.TimeslotResp> listTimeslots(Long scenicId) {
        return timeslotMapper.listByScenicId(scenicId).stream().map(this::toTimeslotResp).toList();
    }

    @Transactional
    /**
     * 创建景区时段。
     */
    public Long createTimeslot(ScenicDto.TimeslotUpsertReq req) {
        Timeslot t = new Timeslot();
        t.setScenicId(req.scenicId());
        t.setName(req.name());
        t.setStartTime(req.startTime());
        t.setEndTime(req.endTime());
        t.setStatus(1);
        timeslotMapper.insert(t);
        return t.getId();
    }

    @Transactional
    /**
     * 更新景区时段。
     */
    public void updateTimeslot(Long id, ScenicDto.TimeslotUpsertReq req) {
        Timeslot t = timeslotMapper.findById(id);
        if (t == null) throw new BizException("时段不存在");
        t.setName(req.name());
        t.setStartTime(req.startTime());
        t.setEndTime(req.endTime());
        timeslotMapper.update(t);
    }

    /**
     * 禁用指定时段。
     */
    public void disableTimeslot(Long id) {
        timeslotMapper.updateStatus(id, 0);
    }

    /**
     * 删除指定时段。
     */
    public void deleteTimeslot(Long id) {
        timeslotMapper.deleteById(id);
    }

    // ========== RefundRule ==========

    /**
     * 查询退改规则列表。
     */
    public List<ScenicDto.RefundRuleResp> listRefundRules(Long scenicId) {
        return refundRuleMapper.listByScenicId(scenicId).stream().map(this::toRefundRuleResp).toList();
    }

    @Transactional
    /**
     * 创建退改规则。
     */
    public Long createRefundRule(ScenicDto.RefundRuleUpsertReq req) {
        RefundRule r = new RefundRule();
        r.setScenicId(req.scenicId());
        r.setName(req.name());
        r.setFreeRefundHours(req.freeRefundHours());
        r.setAllowReschedule(req.allowReschedule());
        refundRuleMapper.insert(r);
        return r.getId();
    }

    @Transactional
    /**
     * 更新退改规则。
     */
    public void updateRefundRule(Long id, ScenicDto.RefundRuleUpsertReq req) {
        RefundRule r = refundRuleMapper.findById(id);
        if (r == null) throw new BizException("退改规则不存在");
        r.setName(req.name());
        r.setFreeRefundHours(req.freeRefundHours());
        r.setAllowReschedule(req.allowReschedule());
        refundRuleMapper.update(r);
    }

    /**
     * 删除退改规则。
     */
    public void deleteRefundRule(Long id) {
        if (refundRuleMapper.findById(id) == null) throw new BizException("退改规则不存在");
        refundRuleMapper.deleteById(id);
    }

    // ========== Converters ==========

    /**
     * 转换景区实体为返回对象。
     */
    private ScenicDto.ScenicResp toScenicResp(ScenicArea s) {
        return new ScenicDto.ScenicResp(s.getId(), s.getName(), s.getAddress(),
            s.getOpenTimeDesc(), s.getContactPhone(), s.getStatus());
    }

    /**
     * 转换时段实体为返回对象。
     */
    private ScenicDto.TimeslotResp toTimeslotResp(Timeslot t) {
        return new ScenicDto.TimeslotResp(t.getId(), t.getScenicId(), t.getName(),
            t.getStartTime(), t.getEndTime(), t.getStatus());
    }

    /**
     * 转换退改规则实体为返回对象。
     */
    private ScenicDto.RefundRuleResp toRefundRuleResp(RefundRule r) {
        return new ScenicDto.RefundRuleResp(r.getId(), r.getScenicId(), r.getName(),
            r.getFreeRefundHours(), r.getAllowReschedule());
    }
}
