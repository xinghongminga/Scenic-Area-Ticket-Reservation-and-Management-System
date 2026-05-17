package com.example.scencispotback.config;

import com.example.scencispotback.domain.AuditLog;
import com.example.scencispotback.mapper.AuditLogMapper;
import com.example.scencispotback.security.LoginUser;
import com.example.scencispotback.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
// 审计切面
public class AuditLogAspect {

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
        "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
        "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void auditableMethods() {
    }

    @After("auditableMethods()")
    public void recordAuditLog(JoinPoint joinPoint) {
        try {
            LoginUser user = UserContext.get();
            if (user == null) {
                return;
            }

            Long operatorId = user.userId();
            String operatorRole = user.role();
            if (operatorId == null) {
                return;
            }

            String methodName = joinPoint.getSignature().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String module = parseModule(className);
            String action = parseAction(methodName);

            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attrs == null ? null : attrs.getRequest();
            String ip = request == null ? "UNKNOWN" : getClientIp(request);

            AuditLog log = new AuditLog();
            log.setOperatorId(operatorId);
            log.setOperatorRole(operatorRole);
            log.setModule(module);
            log.setAction(action);
            log.setIp(ip);
            log.setCreatedAt(LocalDateTime.now());
            auditLogMapper.insert(log);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String parseModule(String className) {
        if (className.contains("Ticket")) return "TICKET";
        if (className.contains("Order")) return "ORDER";
        if (className.contains("Scenic")) return "SCENIC";
        if (className.contains("User")) return "USER";
        if (className.contains("Aftersale")) return "AFTERSALE";
        if (className.contains("Video")) return "VIDEO";
        return "OTHER";
    }

    private String parseAction(String methodName) {
        if (methodName.contains("create") || methodName.contains("add")) return "CREATE";
        if (methodName.contains("update") || methodName.contains("edit")) return "UPDATE";
        if (methodName.contains("delete") || methodName.contains("remove")) return "DELETE";
        if (methodName.contains("status")) return "STATUS_CHANGE";
        return "OTHER";
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
