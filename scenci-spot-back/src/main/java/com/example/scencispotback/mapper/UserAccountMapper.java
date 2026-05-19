package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.UserAccount;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
// 用户映射
public interface UserAccountMapper {

    UserAccount findById(@Param("id") Long id);

    List<UserAccount> listFiltered(@Param("keyword") String keyword,
                                   @Param("role") String role,
                                   @Param("status") Integer status);

    int updateProfile(@Param("id") Long id,
                      @Param("nickname") String nickname,
                      @Param("fullName") String fullName,
                      @Param("idCardNo") String idCardNo,
                      @Param("scenicId") Long scenicId,
                      @Param("role") String role);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    UserAccount findByPhone(@Param("phone") String phone);

    UserAccount findByUsername(@Param("username") String username);

    UserAccount findByOauth(@Param("provider") String provider, @Param("openId") String openId);

    int insert(UserAccount user);

    int insertOauthUser(@Param("role") String role,
                        @Param("scenicId") Long scenicId,
                        @Param("status") Integer status,
                        @Param("loginType") String loginType,
                        @Param("nickname") String nickname,
                        @Param("avatarUrl") String avatarUrl,
                        @Param("provider") String provider,
                        @Param("openId") String openId);

    int updateOauthProfile(@Param("id") Long id,
                           @Param("nickname") String nickname,
                           @Param("avatarUrl") String avatarUrl);

    int updateMyProfile(@Param("id") Long id,
                        @Param("nickname") String nickname,
                        @Param("avatarUrl") String avatarUrl,
                        @Param("phone") String phone);

    List<UserAccount> listActiveByRoles(@Param("roles") List<String> roles);
}
