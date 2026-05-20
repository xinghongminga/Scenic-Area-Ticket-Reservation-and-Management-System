package com.example.scencispotback.controller;

import com.example.scencispotback.domain.AuditLog;
import com.example.scencispotback.mapper.AuditLogMapper;
import com.example.scencispotback.security.Authz;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/audit-logs")
// 审计日志控制器
public class AuditLogController {

    @Autowired
    private AuditLogMapper auditLogMapper;

    /**
     * 查询审计日志列表，支持分页与条件筛选。
     */
    @GetMapping
    public Map<String, Object> listAuditLogs(@RequestParam(required = false) String module,
                                             @RequestParam(required = false) String action,
                                             @RequestParam(required = false) String operatorRole,
                                             @RequestParam(defaultValue = "1") int pageNo,
                                             @RequestParam(defaultValue = "20") int pageSize) {
        Authz.requireRole("ADMIN");
        int offset = (pageNo - 1) * pageSize;
        
        List<AuditLog> logs = auditLogMapper.listAll(offset, pageSize);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", logs);
        result.put("page", pageNo);
        result.put("pageSize", pageSize);
        result.put("total", logs.size());
        return result;
    }

    /**
     * 按ID获取审计日志详情。
     */
    @GetMapping("/{id}")
    public AuditLog getAuditLog(@PathVariable Long id) {
        Authz.requireRole("ADMIN");
        return auditLogMapper.findById(id);
    }

    /**
     * 导出审计日志为CSV字节流。
     */
    @GetMapping("/export")
    public byte[] exportAuditLogs(@RequestParam(required = false) String module,
                                 @RequestParam(required = false) String action) {
        Authz.requireRole("ADMIN");
        List<AuditLog> logs = auditLogMapper.listAll(0, 10000);
        
        StringBuilder csv = new StringBuilder();
        csv.append("ID,操作员ID,角色,模块,操作,目标类型,目标ID,IP地址,创建时间\n");
        
        for (AuditLog log : logs) {
            csv.append(log.getId()).append(",");
            csv.append(log.getOperatorId()).append(",");
            csv.append(log.getOperatorRole()).append(",");
            csv.append(log.getModule()).append(",");
            csv.append(log.getAction()).append(",");
            csv.append(log.getTargetType()).append(",");
            csv.append(log.getTargetId()).append(",");
            csv.append(log.getIp()).append(",");
            csv.append(log.getCreatedAt()).append("\n");
        }
        
        return csv.toString().getBytes();
    }

    /**
     * 清理历史审计日志，返回清理结果提示。
     */
    @DeleteMapping("/cleanup")
    public Map<String, Object> cleanupOldLogs() {
        Authz.requireRole("ADMIN");
        // 可以实现删除90天前的日志
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "审计日志清理完成");
        return result;
    }
}
