package com.example.scencispotback.security;

import com.example.scencispotback.common.BizException;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class Authz {
    private Authz() {
    }

    public static void requireRole(String... roles) {
        LoginUser user = UserContext.get();
        if (user == null) {
            throw new BizException("未登录");
        }
        Set<String> allow = Arrays.stream(roles).collect(Collectors.toSet());
        if (!allow.contains(user.role())) {
            throw new BizException("无权限访问");
        }
    }
}
