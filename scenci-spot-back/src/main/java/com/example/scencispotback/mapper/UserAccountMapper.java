package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.UserAccount;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
// 用户映射
public interface UserAccountMapper {

    @Select("select * from user_account where id = #{id}")
    UserAccount findById(@Param("id") Long id);

    @Select({"<script>",
        "select * from user_account where 1=1",
        "<if test='keyword != null and keyword != \"\"'> and (phone like concat('%',#{keyword},'%') or username like concat('%',#{keyword},'%') or nickname like concat('%',#{keyword},'%') or full_name like concat('%',#{keyword},'%') or id_card_no like concat('%',#{keyword},'%'))</if>",
        "<if test='role != null and role != \"\"'> and role = #{role}</if>",
        "<if test='status != null'> and status = #{status}</if>",
        "order by id desc",
        "</script>"})
    List<UserAccount> listFiltered(@Param("keyword") String keyword,
                                   @Param("role") String role,
                                   @Param("status") Integer status);

    @Update("update user_account set nickname=#{nickname}, full_name=#{fullName}, id_card_no=#{idCardNo}, scenic_id=#{scenicId}, role=#{role}, updated_at=now() where id=#{id}")
    int updateProfile(@Param("id") Long id,
                      @Param("nickname") String nickname,
                      @Param("fullName") String fullName,
                      @Param("idCardNo") String idCardNo,
                      @Param("scenicId") Long scenicId,
                      @Param("role") String role);

    @Update("update user_account set password_hash=#{password}, updated_at=now() where id=#{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Update("update user_account set status=#{status}, updated_at=now() where id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Select("select * from user_account where phone = #{phone} limit 1")
    UserAccount findByPhone(@Param("phone") String phone);

    @Select("select * from user_account where username = #{username} limit 1")
    UserAccount findByUsername(@Param("username") String username);

    @Select("select * from user_account where oauth_provider = #{provider} and oauth_open_id = #{openId} limit 1")
    UserAccount findByOauth(@Param("provider") String provider, @Param("openId") String openId);

    @Insert("insert into user_account(role, scenic_id, status, login_type, phone, username, password_hash, nickname, full_name, id_card_no) " +
        "values(#{role}, #{scenicId}, #{status}, #{loginType}, #{phone}, #{username}, #{passwordHash}, #{nickname}, #{fullName}, #{idCardNo})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserAccount user);

    @Insert("insert into user_account(role, scenic_id, status, login_type, nickname, avatar_url, oauth_provider, oauth_open_id) " +
        "values(#{role}, #{scenicId}, #{status}, #{loginType}, #{nickname}, #{avatarUrl}, #{provider}, #{openId})")
    int insertOauthUser(@Param("role") String role,
                        @Param("scenicId") Long scenicId,
                        @Param("status") Integer status,
                        @Param("loginType") String loginType,
                        @Param("nickname") String nickname,
                        @Param("avatarUrl") String avatarUrl,
                        @Param("provider") String provider,
                        @Param("openId") String openId);

    @Update("update user_account set nickname=#{nickname}, avatar_url=#{avatarUrl}, updated_at=now() where id=#{id}")
    int updateOauthProfile(@Param("id") Long id,
                           @Param("nickname") String nickname,
                           @Param("avatarUrl") String avatarUrl);

    @Update("update user_account set nickname=#{nickname}, avatar_url=#{avatarUrl}, phone=#{phone}, updated_at=now() where id=#{id}")
    int updateMyProfile(@Param("id") Long id,
                        @Param("nickname") String nickname,
                        @Param("avatarUrl") String avatarUrl,
                        @Param("phone") String phone);

    @Select({"<script>",
        "select * from user_account where status=1 and role in",
        "<foreach collection='roles' item='role' open='(' separator=',' close=')'>#{role}</foreach>",
        "order by id asc",
        "</script>"})
    List<UserAccount> listActiveByRoles(@Param("roles") List<String> roles);
}
