package com.example.scencispotback.api.user;

import com.example.scencispotback.common.ApiResponse;
import com.example.scencispotback.security.Authz;
import com.example.scencispotback.service.UserMgmtService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 * 提供管理员用户管理、当前用户个人信息修改等接口
 */
@RestController
public class UserMgmtController {

    private final UserMgmtService userMgmtService;

    /**
     * 构造注入用户管理服务
     * @param userMgmtService 用户管理业务服务
     */
    public UserMgmtController(UserMgmtService userMgmtService) {
        this.userMgmtService = userMgmtService;
    }

    // ======================== 管理员：用户管理模块 ========================

    /**
     * 管理员分页/条件查询用户列表
     * 支持关键词、角色、状态筛选
     * @param keyword 搜索关键词（用户名/昵称）
     * @param role 角色筛选
     * @param status 状态筛选
     * @return 用户列表响应
     */
    @GetMapping("/api/admin/users")
    public ApiResponse<List<UserMgmtDto.UserResp>> list(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String role,
                                                        @RequestParam(required = false) Integer status) {
        // 仅管理员可访问
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(userMgmtService.list(new UserMgmtDto.UserQuery(keyword, role, status)));
    }

    /**
     * 管理员根据用户ID查询单个用户详情
     * @param id 用户ID
     * @return 用户详情响应
     */
    @GetMapping("/api/admin/users/{id}")
    public ApiResponse<UserMgmtDto.UserResp> getById(@PathVariable Long id) {
        // 仅管理员可访问
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(userMgmtService.getById(id));
    }

    /**
     * 管理员创建员工账号（内部人员）
     * @param req 创建员工请求参数
     * @return 新创建用户的ID
     */
    @PostMapping("/api/admin/users/staff")
    public ApiResponse<Map<String, Long>> createStaff(@Valid @RequestBody UserMgmtDto.CreateStaffReq req) {
        // 仅管理员可访问
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(Map.of("id", userMgmtService.createStaff(req)));
    }

    /**
     * 管理员更新用户基本信息
     * @param id 用户ID
     * @param req 用户更新信息
     * @return 无返回数据
     */
    @PutMapping("/api/admin/users/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody UserMgmtDto.UpdateUserReq req) {
        // 仅管理员可访问
        Authz.requireRole("ADMIN");
        userMgmtService.update(id, req);
        return ApiResponse.ok(null);
    }

    /**
     * 管理员重置用户密码
     * @param id 用户ID
     * @param req 新密码参数
     * @return 无返回数据
     */
    @PutMapping("/api/admin/users/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody UserMgmtDto.ResetPasswordReq req) {
        // 仅管理员可访问
        Authz.requireRole("ADMIN");
        userMgmtService.resetPassword(id, req.newPassword());
        return ApiResponse.ok(null);
    }

    /**
     * 管理员修改用户启用/禁用状态
     * @param id 用户ID
     * @param req 状态参数
     * @return 无返回数据
     */
    @PutMapping("/api/admin/users/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserMgmtDto.UserStatusReq req) {
        // 仅管理员可访问
        Authz.requireRole("ADMIN");
        userMgmtService.updateStatus(id, req.status());
        return ApiResponse.ok(null);
    }

    // ======================== 当前登录用户：个人中心 ========================

    /**
     * 当前登录用户修改个人资料
     * 可修改：昵称、头像、手机号
     * @param body 个人资料参数
     * @return 无返回数据
     */
    @PutMapping("/api/user/profile")
    public ApiResponse<Void> updateMyProfile(@RequestBody Map<String, String> body) {
        // 所有合法角色均可访问
        Authz.requireRole("TOURIST", "ADMIN", "ANALYST", "AUDITOR");
        userMgmtService.updateMyProfile(body.get("nickname"), body.get("avatarUrl"), body.get("phone"));
        return ApiResponse.ok(null);
    }

    /**
     * 当前登录用户修改自己的密码
     * 需要验证旧密码
     * @param body 旧密码+新密码参数
     * @return 无返回数据
     */
    @PutMapping("/api/user/password")
    public ApiResponse<Void> changeMyPassword(@RequestBody Map<String, String> body) {
        // 所有合法角色均可访问
        Authz.requireRole("TOURIST", "ADMIN", "ANALYST", "AUDITOR");
        userMgmtService.changeMyPassword(body.get("oldPassword"), body.get("newPassword"));
        return ApiResponse.ok(null);
    }
}
