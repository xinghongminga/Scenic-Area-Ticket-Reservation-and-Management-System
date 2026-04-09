package com.example.scencispotback.api.user;

import com.example.scencispotback.common.ApiResponse;
import com.example.scencispotback.security.Authz;
import com.example.scencispotback.service.UserMgmtService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class UserMgmtController {

    private final UserMgmtService userMgmtService;

    public UserMgmtController(UserMgmtService userMgmtService) {
        this.userMgmtService = userMgmtService;
    }

    // ===== Admin: User Management =====

    @GetMapping("/api/admin/users")
    public ApiResponse<List<UserMgmtDto.UserResp>> list(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String role,
                                                        @RequestParam(required = false) Integer status) {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(userMgmtService.list(new UserMgmtDto.UserQuery(keyword, role, status)));
    }

    @GetMapping("/api/admin/users/{id}")
    public ApiResponse<UserMgmtDto.UserResp> getById(@PathVariable Long id) {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(userMgmtService.getById(id));
    }

    @PostMapping("/api/admin/users/staff")
    public ApiResponse<Map<String, Long>> createStaff(@Valid @RequestBody UserMgmtDto.CreateStaffReq req) {
        Authz.requireRole("ADMIN");
        return ApiResponse.ok(Map.of("id", userMgmtService.createStaff(req)));
    }

    @PutMapping("/api/admin/users/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody UserMgmtDto.UpdateUserReq req) {
        Authz.requireRole("ADMIN");
        userMgmtService.update(id, req);
        return ApiResponse.ok(null);
    }

    @PutMapping("/api/admin/users/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody UserMgmtDto.ResetPasswordReq req) {
        Authz.requireRole("ADMIN");
        userMgmtService.resetPassword(id, req.newPassword());
        return ApiResponse.ok(null);
    }

    @PutMapping("/api/admin/users/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserMgmtDto.UserStatusReq req) {
        Authz.requireRole("ADMIN");
        userMgmtService.updateStatus(id, req.status());
        return ApiResponse.ok(null);
    }

    // ===== Current User Profile =====

    @PutMapping("/api/user/profile")
    public ApiResponse<Void> updateMyProfile(@RequestBody Map<String, String> body) {
        Authz.requireRole("TOURIST", "ADMIN", "ANALYST", "AUDITOR");
        userMgmtService.updateMyProfile(body.get("nickname"));
        return ApiResponse.ok(null);
    }

    @PutMapping("/api/user/password")
    public ApiResponse<Void> changeMyPassword(@RequestBody Map<String, String> body) {
        Authz.requireRole("TOURIST", "ADMIN", "ANALYST", "AUDITOR");
        userMgmtService.changeMyPassword(body.get("oldPassword"), body.get("newPassword"));
        return ApiResponse.ok(null);
    }
}
