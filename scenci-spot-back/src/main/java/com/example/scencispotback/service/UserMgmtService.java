package com.example.scencispotback.service;

import com.example.scencispotback.api.user.UserMgmtDto;
import com.example.scencispotback.common.BizException;
import com.example.scencispotback.domain.UserAccount;
import com.example.scencispotback.mapper.UserAccountMapper;
import com.example.scencispotback.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserMgmtService {

    private final UserAccountMapper userAccountMapper;

    public UserMgmtService(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    public List<UserMgmtDto.UserResp> list(UserMgmtDto.UserQuery query) {
        return userAccountMapper.listFiltered(query.keyword(), query.role(), query.status())
            .stream().map(this::toResp).toList();
    }

    public UserMgmtDto.UserResp getById(Long id) {
        UserAccount u = userAccountMapper.findById(id);
        if (u == null) throw new BizException("用户不存在");
        return toResp(u);
    }

    @Transactional
    public Long createStaff(UserMgmtDto.CreateStaffReq req) {
        if (!List.of("ADMIN", "ANALYST", "AUDITOR").contains(req.role())) {
            throw new BizException("角色只允许 ADMIN/ANALYST/AUDITOR");
        }
        UserAccount u = new UserAccount();
        u.setRole(req.role());
        u.setScenicId(req.scenicId());
        u.setStatus(1);
        u.setLoginType("ACCOUNT");
        u.setUsername(req.username());
        u.setPasswordHash(req.password());
        u.setNickname(req.nickname() == null ? req.username() : req.nickname());
        userAccountMapper.insert(u);
        return u.getId();
    }

    @Transactional
    public void update(Long id, UserMgmtDto.UpdateUserReq req) {
        UserAccount u = userAccountMapper.findById(id);
        if (u == null) throw new BizException("用户不存在");
        String role = req.role() != null ? req.role() : u.getRole();
        Long scenicId = req.scenicId() != null ? req.scenicId() : u.getScenicId();
        String nickname = req.nickname() != null ? req.nickname() : u.getNickname();
        String fullName = req.fullName() != null ? req.fullName() : u.getFullName();
        String idCardNo = req.idCardNo() != null ? req.idCardNo() : u.getIdCardNo();
        userAccountMapper.updateProfile(id, nickname, fullName, idCardNo, scenicId, role);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        UserAccount u = userAccountMapper.findById(id);
        if (u == null) throw new BizException("用户不存在");
        userAccountMapper.updatePassword(id, newPassword);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        Long operatorId = UserContext.get().userId();
        if (id.equals(operatorId)) throw new BizException("不能禁用自己的账号");
        if (userAccountMapper.updateStatus(id, status) == 0) throw new BizException("用户不存在");
    }

    @Transactional
    public void updateMyProfile(String nickname) {
        Long userId = UserContext.get().userId();
        UserAccount u = userAccountMapper.findById(userId);
        if (u == null) throw new BizException("用户不存在");
        userAccountMapper.updateProfile(userId,
            nickname != null ? nickname : u.getNickname(),
            u.getFullName(),
            u.getIdCardNo(),
            u.getScenicId(), u.getRole());
    }

    @Transactional
    public void changeMyPassword(String oldPassword, String newPassword) {
        Long userId = UserContext.get().userId();
        UserAccount u = userAccountMapper.findById(userId);
        if (u == null) throw new BizException("用户不存在");
        if (u.getPasswordHash() == null || !u.getPasswordHash().equals(oldPassword)) {
            throw new BizException("原密码错误");
        }
        userAccountMapper.updatePassword(userId, newPassword);
    }

    private UserMgmtDto.UserResp toResp(UserAccount u) {
        return new UserMgmtDto.UserResp(u.getId(), u.getRole(), u.getScenicId(),
            u.getStatus(), u.getLoginType(), u.getPhone(), u.getUsername(),
            u.getNickname(), u.getFullName(), u.getIdCardNo(), u.getCreatedAt());
    }
}
