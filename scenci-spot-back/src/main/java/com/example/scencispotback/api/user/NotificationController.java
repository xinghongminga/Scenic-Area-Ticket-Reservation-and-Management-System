package com.example.scencispotback.api.user;

import com.example.scencispotback.common.ApiResponse;
import com.example.scencispotback.common.BizException;
import com.example.scencispotback.security.UserContext;
import com.example.scencispotback.security.LoginUser;
import com.example.scencispotback.domain.Notification;
import com.example.scencispotback.mapper.NotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/notifications")
// 通知控制器
public class NotificationController {

    @Autowired
    private NotificationMapper notificationMapper;

    /**
        * 获取当前用户的通知列表（分页）。
     */
    @GetMapping
    public ApiResponse<List<Notification>> listNotifications(@RequestParam(defaultValue = "1") int pageNo,
                                                             @RequestParam(defaultValue = "10") int pageSize) {
        Long receiverId = currentUserId();
        List<Notification> all = notificationMapper.listForUser(receiverId);
        int from = Math.max(0, (pageNo - 1) * pageSize);
        if (from >= all.size()) {
            return ApiResponse.ok(List.of());
        }
        int to = Math.min(all.size(), from + pageSize);
        return ApiResponse.ok(all.subList(from, to));
    }

    /**
     * 获取未读通知数量。
     */
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Object>> getUnreadCount() {
        Long receiverId = currentUserId();
        int count = notificationMapper.countUnread(receiverId);
        Map<String, Object> result = new HashMap<>();
        result.put("unreadCount", count);
        return ApiResponse.ok(result);
    }

    /**
     * 获取通知详情。
     */
    @GetMapping("/{id}")
    public ApiResponse<Notification> getDetail(@PathVariable Long id) {
        Notification notification = requireOwnedNotification(id);
        return ApiResponse.ok(notification);
    }

    /**
     * 标记通知为已读。
     */
    @PutMapping("/{id}/read")
    public ApiResponse<Map<String, Object>> markAsRead(@PathVariable Long id) {
        requireOwnedNotification(id);
        notificationMapper.markAsRead(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ApiResponse.ok(result);
    }

    /**
     * 标记当前用户全部通知为已读。
     */
    @PutMapping("/mark-all-read")
    public ApiResponse<Map<String, Object>> markAllRead() {
        Long receiverId = currentUserId();
        notificationMapper.markAllAsRead(receiverId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ApiResponse.ok(result);
    }

    /**
     * 获取最新的5条通知。
     */
    @GetMapping("/latest")
    public ApiResponse<List<Notification>> getLatestNotifications() {
        Long receiverId = currentUserId();
        List<Notification> all = notificationMapper.listForUser(receiverId);
        return ApiResponse.ok(all.stream().limit(5).toList());
    }

    /**
     * 删除单条通知。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> deleteNotification(@PathVariable Long id) {
        requireOwnedNotification(id);
        // 可以添加删除逻辑，如果NotificationMapper支持
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ApiResponse.ok(result);
    }

    /**
     * 获取当前登录用户ID。
     */
    private Long currentUserId() {
        LoginUser user = UserContext.get();
        if (user == null || user.userId() == null) {
            throw new BizException("请先登录");
        }
        return user.userId();
    }

    /**
     * 校验通知归属并返回通知详情。
     */
    private Notification requireOwnedNotification(Long id) {
        Notification notification = notificationMapper.findById(id);
        if (notification == null || !currentUserId().equals(notification.getReceiverId())) {
            throw new IllegalArgumentException("通知不存在");
        }
        return notification;
    }
}
