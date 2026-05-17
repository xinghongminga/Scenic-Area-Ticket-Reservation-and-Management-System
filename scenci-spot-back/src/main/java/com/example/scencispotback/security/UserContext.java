package com.example.scencispotback.security;

/**
 * 用户上下文工具类
 * 基于 ThreadLocal 存储当前登录用户信息，保证线程安全
 * 用于在整个请求链路中获取当前登录用户的身份信息
 */
public final class UserContext {

    /**
     * 线程局部变量，存储当前线程的登录用户信息
     * 每个请求独立线程，互不干扰
     */
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    /**
     * 私有构造方法，禁止实例化工具类
     */
    private UserContext() {
    }

    /**
     * 设置当前登录用户信息
     * @param user 登录用户对象
     */
    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    /**
     * 获取当前线程的登录用户信息
     * @return 登录用户对象，未登录时返回 null
     */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /**
     * 清除当前线程的用户信息
     * 请求结束后必须调用，防止内存泄漏
     */
    public static void clear() {
        HOLDER.remove();
    }
}