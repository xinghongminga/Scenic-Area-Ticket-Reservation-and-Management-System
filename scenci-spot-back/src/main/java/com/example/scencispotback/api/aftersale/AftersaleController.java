package com.example.scencispotback.api.aftersale;

import com.example.scencispotback.common.ApiResponse;
import com.example.scencispotback.security.Authz;
import com.example.scencispotback.service.AftersaleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 售后工单控制器
 * 提供游客提交售后、查询个人售后、审核员/管理员审核、管理售后工单等接口
 */
@RestController
public class AftersaleController {

    private final AftersaleService aftersaleService;

    /**
     * 构造注入售后业务服务
     */
    public AftersaleController(AftersaleService aftersaleService) {
        this.aftersaleService = aftersaleService;
    }

    /**
     * 游客提交售后申请（退票/改期）
     * 权限：仅游客
     * @param req 售后申请参数
     * @return 生成的售后申请单号 reqNo
     */
    @PostMapping("/api/aftersale")
    public ApiResponse<Map<String, String>> create(@Valid @RequestBody AftersaleDto.CreateReq req) {
        Authz.requireRole("TOURIST");
        String reqNo = aftersaleService.submit(req);
        return ApiResponse.ok(Map.of("reqNo", reqNo));
    }

    /**
     * 游客查询自己的所有售后申请记录
     * 权限：仅游客
     * @return 当前用户的售后列表
     */
    @GetMapping("/api/aftersale/my")
    public ApiResponse<List<AftersaleDto.ReqResp>> myList() {
        Authz.requireRole("TOURIST");
        return ApiResponse.ok(aftersaleService.myList());
    }

    /**
     * 游客获取指定订单可改期的选项（日期/场次）
     * 权限：仅游客
     * @param orderNo 订单编号
     * @return 可改期选项列表
     */
    @GetMapping("/api/aftersale/reschedule/options")
    public ApiResponse<List<AftersaleDto.RescheduleOptionResp>> rescheduleOptions(@RequestParam String orderNo) {
        Authz.requireRole("TOURIST");
        return ApiResponse.ok(aftersaleService.rescheduleOptions(orderNo));
    }

    /**
     * 审核员/管理员 查询所有用户的售后申请
     * 支持按用户手机号、工单状态筛选
     * 权限：审核员、管理员
     * @param userPhone 用户手机号（可选）
     * @param status 工单状态（可选）
     * @return 全量售后工单列表
     */
    @GetMapping("/api/auditor/aftersale")
    public ApiResponse<List<AftersaleDto.ReqResp>> allList(@RequestParam(required = false) String userPhone,
                                                           @RequestParam(required = false) String status) {
        Authz.requireRole("AUDITOR", "ADMIN");
        return ApiResponse.ok(aftersaleService.allList(new AftersaleDto.QueryReq(userPhone, status)));
    }

    /**
     * 审核员/管理员 更新售后工单信息
     * 权限：审核员、管理员
     * @param reqNo 售后单号
     * @param req 更新参数
     * @return 无返回数据
     */
    @PutMapping("/api/auditor/aftersale/{reqNo}")
    public ApiResponse<Void> update(@PathVariable String reqNo, @RequestBody AftersaleDto.UpdateReq req) {
        Authz.requireRole("AUDITOR", "ADMIN");
        aftersaleService.updateByAuditor(reqNo, req);
        return ApiResponse.ok(null);
    }

    /**
     * 审核员/管理员 删除售后工单
     * 权限：审核员、管理员
     * @param reqNo 售后单号
     * @return 无返回数据
     */
    @DeleteMapping("/api/auditor/aftersale/{reqNo}")
    public ApiResponse<Void> delete(@PathVariable String reqNo) {
        Authz.requireRole("AUDITOR", "ADMIN");
        aftersaleService.deleteByAuditor(reqNo);
        return ApiResponse.ok(null);
    }

    /**
     * 审核员/管理员 审核通过售后申请
     * 权限：审核员、管理员
     * @param reqNo 售后单号
     * @param req 审核意见（可选）
     * @return 无返回数据
     */
    @PostMapping("/api/auditor/aftersale/{reqNo}/approve")
    public ApiResponse<Void> approve(@PathVariable String reqNo, @RequestBody(required = false) AftersaleDto.AuditReq req) {
        Authz.requireRole("AUDITOR", "ADMIN");
        aftersaleService.approve(reqNo, req == null ? null : req.auditComment());
        return ApiResponse.ok(null);
    }

    /**
     * 审核员/管理员 审核驳回售后申请
     * 权限：审核员、管理员
     * @param reqNo 售后单号
     * @param req 审核意见（可选）
     * @return 无返回数据
     */
    @PostMapping("/api/auditor/aftersale/{reqNo}/reject")
    public ApiResponse<Void> reject(@PathVariable String reqNo, @RequestBody(required = false) AftersaleDto.AuditReq req) {
        Authz.requireRole("AUDITOR", "ADMIN");
        aftersaleService.reject(reqNo, req == null ? null : req.auditComment());
        return ApiResponse.ok(null);
    }
}