package com.example.scencispotback.service;

import com.example.scencispotback.api.auth.AuthDto;
import com.example.scencispotback.common.BizException;
import com.example.scencispotback.domain.UserAccount;
import com.example.scencispotback.mapper.UserAccountMapper;
import com.example.scencispotback.security.LoginUser;
import com.example.scencispotback.security.TokenService;
import com.example.scencispotback.security.UserContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Random;

@Service
public class AuthService {

    private final StringRedisTemplate redisTemplate;
    private final UserAccountMapper userAccountMapper;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;
    private final long codeTtlSeconds;
    private final String wechatMiniAppId;
    private final String wechatMiniAppSecret;
    private final Random random = new Random();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public AuthService(StringRedisTemplate redisTemplate,
                       UserAccountMapper userAccountMapper,
                       TokenService tokenService,
                       ObjectMapper objectMapper,
                       @Value("${app.auth.code-ttl-seconds}") long codeTtlSeconds,
                       @Value("${app.wechat.mini.app-id:}") String wechatMiniAppId,
                       @Value("${app.wechat.mini.app-secret:}") String wechatMiniAppSecret) {
        this.redisTemplate = redisTemplate;
        this.userAccountMapper = userAccountMapper;
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
        this.codeTtlSeconds = codeTtlSeconds;
        this.wechatMiniAppId = wechatMiniAppId;
        this.wechatMiniAppSecret = wechatMiniAppSecret;
    }

    public String sendCode(String phone) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        redisTemplate.opsForValue().set(codeKey(phone), code, Duration.ofSeconds(codeTtlSeconds));
        return code;
    }

    public AuthDto.LoginResp registerByCode(AuthDto.RegisterByCodeReq req) {
        String expected = redisTemplate.opsForValue().get(codeKey(req.phone()));
        if (expected == null || !expected.equals(req.code())) {
            throw new BizException("验证码错误或已过期");
        }
        UserAccount exists = userAccountMapper.findByPhone(req.phone());
        if (exists != null) {
            throw new BizException("手机号已注册，请直接登录");
        }
        UserAccount user = new UserAccount();
        user.setRole("TOURIST");
        user.setStatus(1);
        user.setLoginType("PHONE");
        user.setPhone(req.phone());
        String username = (req.username() == null || req.username().isBlank()) ? req.phone() : req.username();
        user.setUsername(username);
        if (req.password() != null && !req.password().isBlank()) {
            user.setPasswordHash(req.password());
        }
        user.setNickname(req.nickname() == null || req.nickname().isBlank() ? "游客" + req.phone().substring(req.phone().length() - 4) : req.nickname());
        user.setFullName(req.fullName());
        user.setIdCardNo(req.idCardNo());
        userAccountMapper.insert(user);
        return loginResp(user);
    }

    public AuthDto.LoginResp loginByCode(AuthDto.LoginByCodeReq req) {
        String expected = redisTemplate.opsForValue().get(codeKey(req.phone()));
        if (expected == null || !expected.equals(req.code())) {
            throw new BizException("验证码错误或已过期");
        }
        UserAccount user = userAccountMapper.findByPhone(req.phone());
        if (user == null) {
            throw new BizException("账号未注册，请先注册");
        }
        return loginResp(user);
    }

    public AuthDto.LoginResp loginByPassword(AuthDto.LoginByPasswordReq req) {
        UserAccount user = userAccountMapper.findByUsername(req.username());
        if (user == null) {
            user = userAccountMapper.findByPhone(req.username());
        }
        if (user == null) {
            throw new BizException("账号不存在");
        }
        if (user.getPasswordHash() == null || !user.getPasswordHash().equals(req.password())) {
            throw new BizException("用户名或密码错误");
        }
        if (req.role() != null && !req.role().isBlank() && !req.role().equalsIgnoreCase(user.getRole())) {
            throw new BizException("身份与账号不匹配");
        }
        return loginResp(user);
    }

    public AuthDto.LoginResp loginByOAuthMock(AuthDto.OAuthMockReq req) {
        UserAccount user = userAccountMapper.findByOauth(req.provider(), req.mockOpenId());
        if (user == null) {
            String nickname = (req.nickname() == null || req.nickname().isBlank()) ? "第三方游客" : req.nickname();
            userAccountMapper.insertOauthUser("TOURIST", null, 1, "OAUTH", nickname, null, req.provider(), req.mockOpenId());
            user = userAccountMapper.findByOauth(req.provider(), req.mockOpenId());
        }
        return loginResp(user);
    }

    public AuthDto.LoginResp loginByWechatMini(AuthDto.WechatMiniLoginReq req) {
        String openId = resolveWechatMiniOpenId(req.code(), req.devOpenId());
        UserAccount user = userAccountMapper.findByOauth("WECHAT_MINI_PROGRAM", openId);
        String nickname = (req.nickname() == null || req.nickname().isBlank()) ? "微信用户" : req.nickname();
        String avatarUrl = req.avatarUrl();
        if (user == null) {
            userAccountMapper.insertOauthUser("TOURIST", null, 1, "OAUTH", nickname, avatarUrl, "WECHAT_MINI_PROGRAM", openId);
            user = userAccountMapper.findByOauth("WECHAT_MINI_PROGRAM", openId);
        } else {
            String nextNickname = (req.nickname() == null || req.nickname().isBlank()) ? user.getNickname() : req.nickname();
            String nextAvatarUrl = (req.avatarUrl() == null || req.avatarUrl().isBlank()) ? user.getAvatarUrl() : req.avatarUrl();
            boolean changed = !equalsNullable(nextNickname, user.getNickname()) || !equalsNullable(nextAvatarUrl, user.getAvatarUrl());
            if (changed) {
                userAccountMapper.updateOauthProfile(user.getId(), nextNickname, nextAvatarUrl);
                user.setNickname(nextNickname);
                user.setAvatarUrl(nextAvatarUrl);
            }
        }
        return loginResp(user);
    }

    public AuthDto.LoginResp me() {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null) {
            throw new BizException("未登录");
        }
        return new AuthDto.LoginResp(null, loginUser.userId(), loginUser.role(), loginUser.nickname(), null);
    }

    private AuthDto.LoginResp loginResp(UserAccount user) {
        String token = tokenService.issueToken(new LoginUser(user.getId(), user.getRole(), user.getScenicId(), user.getNickname()));
        return new AuthDto.LoginResp(token, user.getId(), user.getRole(), user.getNickname(), user.getAvatarUrl());
    }

    private String resolveWechatMiniOpenId(String code, String devOpenId) {
        if (wechatMiniAppId == null || wechatMiniAppId.isBlank() || wechatMiniAppSecret == null || wechatMiniAppSecret.isBlank()) {
            if (devOpenId != null && !devOpenId.isBlank()) {
                return "DEV_" + devOpenId;
            }
            throw new BizException("微信小程序登录未配置 appid 或 secret");
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid="
            + URLEncoder.encode(wechatMiniAppId, StandardCharsets.UTF_8)
            + "&secret=" + URLEncoder.encode(wechatMiniAppSecret, StandardCharsets.UTF_8)
            + "&js_code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
            + "&grant_type=authorization_code";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 400) {
                throw new BizException("微信登录服务异常");
            }
            JsonNode node = objectMapper.readTree(resp.body());
            int errCode = node.path("errcode").asInt(0);
            if (errCode != 0) {
                String errMsg = node.path("errmsg").asText("微信登录失败");
                throw new BizException("微信登录失败: " + errMsg);
            }
            String openId = node.path("openid").asText("");
            if (openId.isBlank()) {
                throw new BizException("微信登录失败: openid 为空");
            }
            return openId;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("微信登录异常: " + e.getMessage());
        }
    }

    private boolean equalsNullable(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    private String codeKey(String phone) {
        return "auth:code:" + phone;
    }
}
