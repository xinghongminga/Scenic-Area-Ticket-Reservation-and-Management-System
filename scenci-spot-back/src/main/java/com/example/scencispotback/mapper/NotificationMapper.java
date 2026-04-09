package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.Notification;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NotificationMapper {

    @Insert("insert into sys_notification(receiver_id, title, content, ntype, created_by) " +
        "values(#{receiverId}, #{title}, #{content}, #{ntype}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Notification notif);

    @Select("select * from sys_notification where receiver_id = #{userId} order by is_read asc, created_at desc")
    List<Notification> listForUser(@Param("userId") Long userId);

    @Select("select * from sys_notification where id = #{id}")
    Notification findById(@Param("id") Long id);

    @Update("update sys_notification set is_read=1 where id=#{id}")
    int markAsRead(@Param("id") Long id);

    @Update("update sys_notification set is_read=1 where receiver_id=#{userId}")
    int markAllAsRead(@Param("userId") Long userId);

    @Select("select count(*) from sys_notification where receiver_id = #{userId} and is_read = 0")
    int countUnread(@Param("userId") Long userId);
}
