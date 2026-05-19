package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 通知映射
public interface NotificationMapper {

    int insert(Notification notif);

    List<Notification> listForUser(@Param("userId") Long userId);

    Notification findById(@Param("id") Long id);

    int markAsRead(@Param("id") Long id);

    int markAllAsRead(@Param("userId") Long userId);

    int countUnread(@Param("userId") Long userId);
}
