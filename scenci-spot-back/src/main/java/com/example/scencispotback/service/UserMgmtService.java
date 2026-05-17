package com.example.scencispotback.service;

import com.example.scencispotback.api.user.UserMgmtDto;
import com.example.scencispotback.common.BizException;
import com.example.scencispotback.domain.UserAccount;
import com.example.scencispotback.mapper.UserAccountMapper;
import com.example.scencispotback.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户管理业务服务
 * 功能：管理员用户管理、个人信息修改、密码修改、状态管理等
 */
@Service
public class UserMgmtService {

    private final UserAccountMapper userAccountMapper;

    /**
     * 构造注入用户账户Mapper
     * @param userAccountMapper 用户账户数据访问接口
     */
    public UserMgmtService(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    /**
     * 条件查询用户列表（管理员）
     * @param query 查询条件：关键词、角色、状态
     * @return 用户响应列表
     */
    public List<UserMgmtDto.UserResp> list(UserMgmtDto.UserQuery query) {
        return userAccountMapper.listFiltered(query.keyword(), query.role(), query.status())
                .stream().map(this::toResp).toList();
    }

    /**
     * 根据ID查询单个用户详情
     * @param id 用户ID
     * @return 用户详情响应对象
     * @throws BizException 用户不存在时抛出
     */
    public UserMgmtDto.UserResp getById(Long id) {
        UserAccount u = userAccountMapper.findById(id);
        if (u == null) throw new BizException("用户不存在");
        return toResp(u);
    }

    /**
     * 管理员创建内部员工账号
     * 仅允许创建 ADMIN/ANALYST/AUDITOR 角色
     * @param req 创建参数
     * @return 新建用户ID
     * @throws BizException 角色不合法时抛出
     */
    @Transactional
    public Long createStaff(UserMgmtDto.CreateStaffReq req) {
        if (!List.of("ADMIN", "ANALYST", "AUDITOR").contains(req.role())) {
            throw new BizException("角色只允许 ADMIN/ANALYST/AUDITOR");
        }
        UserAccount u = new UserAccount();
        u.setRole(req.role());
        u.setScenicId(req.scenicId());
        u.setStatus(1);          // 账号默认启用
        u.setLoginType("ACCOUNT");
        u.setUsername(req.username());
        u.setPasswordHash(req.password());
        u.setNickname(req.nickname() == null ? req.username() : req.nickname());
        userAccountMapper.insert(u);
        return u.getId();
    }

    /**
     * 管理员更新用户基本信息
     * @param id 用户ID
     * @param req 更新信息
     * @throws BizException 用户不存在时抛出
     */
    @Transactional
    public void update(Long id, UserMgmtDto.UpdateUserReq req) {
        UserAccount u = userAccountMapper.findById(id);
        if (u == null) throw new BizException("用户不存在");

        // 为空则使用原有值
        String role = req.role() != null ? req.role() : u.getRole();
        Long scenicId = req.scenicId() != null ? req.scenicId() : u.getScenicId();
        String nickname = req.nickname() != null ? req.nickname() : u.getNickname();
        String fullName = req.fullName() != null ? req.fullName() : u.getFullName();
        String idCardNo = req.idCardNo() != null ? req.idCardNo() : u.getIdCardNo();

        userAccountMapper.updateProfile(id, nickname, fullName, idCardNo, scenicId, role);
    }

    /**
     * 管理员重置用户密码
     * @param id 用户ID
     * @param newPassword 新密码
     * @throws BizException 用户不存在时抛出
     */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        UserAccount u = userAccountMapper.findById(id);
        if (u == null) throw new BizException("用户不存在");
        userAccountMapper.updatePassword(id, newPassword);
    }

    /**
     * 管理员修改用户启用/禁用状态
     * 禁止操作人禁用自己
     * @param id 用户ID
     * @param status 目标状态 1=启用 0=禁用
     * @throws BizException 不能禁用自己 / 用户不存在
     */
    @Transactional
    public void updateStatus(Long id, Integer status) {
        Long operatorId = UserContext.get().userId();
        // 禁止禁用自己账号
        if (id.equals(operatorId)) throw new BizException("不能禁用自己的账号");
        // 更新行数为0表示用户不存在
        if (userAccountMapper.updateStatus(id, status) == 0) throw new BizException("用户不存在");
    }

    /**
     * 当前登录用户修改个人资料
     * 可修改：昵称、头像、手机号
     * 手机号必须唯一
     * @param nickname 昵称
     * @param avatarUrl 头像地址
     * @param phone 手机号
     * @throws BizException 用户不存在 / 手机号已被使用
     */
    @Transactional
    public void updateMyProfile(String nickname, String avatarUrl, String phone) {
        Long userId = UserContext.get().userId();
        UserAccount u = userAccountMapper.findById(userId);
        if (u == null) throw new BizException("用户不存在");

        // 空值则保留原值
        String nextNickname = (nickname == null || nickname.isBlank()) ? u.getNickname() : nickname;
        String nextAvatarUrl = (avatarUrl == null || avatarUrl.isBlank()) ? u.getAvatarUrl() : avatarUrl;
        String nextPhone = (phone == null || phone.isBlank()) ? u.getPhone() : phone;

        // 手机号变更时校验唯一性
        if (nextPhone != null && !nextPhone.equals(u.getPhone())) {
            UserAccount exists = userAccountMapper.findByPhone(nextPhone);
            if (exists != null && !exists.getId().equals(userId)) {
                throw new BizException("手机号已被使用");
            }
        }

        userAccountMapper.updateMyProfile(userId, nextNickname, nextAvatarUrl, nextPhone);
    }

    /**
     * 当前登录用户修改自己的密码
     * 必须验证旧密码
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @throws BizException 用户不存在 / 原密码错误
     */
    @Transactional
    public void changeMyPassword(String oldPassword, String newPassword) {
        Long userId = UserContext.get().userId();
        UserAccount u = userAccountMapper.findById(userId);
        if (u == null) throw new BizException("用户不存在");

        // 校验旧密码
        if (u.getPasswordHash() == null || !u.getPasswordHash().equals(oldPassword)) {
            throw new BizException("原密码错误");
        }

        userAccountMapper.updatePassword(userId, newPassword);
    }

    /**
     * 实体转前端响应对象
     * @param u 用户账户实体
     * @return 接口返回的用户信息
     */
    private UserMgmtDto.UserResp toResp(UserAccount u) {
        return new UserMgmtDto.UserResp(
                u.getId(),
                u.getRole(),
                u.getScenicId(),
                u.getStatus(),
                u.getLoginType(),
                u.getPhone(),
                u.getUsername(),
                u.getNickname(),
                u.getFullName(),
                u.getIdCardNo(),
                u.getCreatedAt()
        );
    }
}