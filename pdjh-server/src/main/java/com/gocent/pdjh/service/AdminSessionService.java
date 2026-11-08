package com.gocent.pdjh.service;

import com.gocent.pdjh.entity.Users;
import com.gocent.pdjh.enums.LoginScene;
import com.gocent.pdjh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 管理后台空闲会话：30 分钟无操作过期，有操作才续期。
 * 消费者：{@code AdminSessionFilter}（增删改查）、{@code UserServiceImpl#getInfo}（刷新/打开页面）。
 * 依赖：users.token、users.token_time；仅 login_scene=ADMIN（空场景按后台账号处理）。护士站/叫号器不走这里。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSessionService {

    /** 与前端 utils/adminSession.js 的 ADMIN_IDLE_MS 保持一致 */
    public static final int IDLE_MINUTES = 30;
    public static final long IDLE_MILLIS = IDLE_MINUTES * 60_000L;
    /** 续期写库间隔，避免每次点击都更新 token_time */
    public static final long RENEW_GAP_MILLIS = 60_000L;
    public static final int EXPIRED_CODE = 50014;
    public static final String EXPIRED_MSG = "登录已过期，请重新登录";

    public enum Check {
        /** 后台会话仍有效 */
        OK,
        /** 非后台账号，调用方勿拦截 */
        SKIP,
        /** 后台会话不存在或已空闲超时，token 已作废 */
        EXPIRED
    }

    private final UserRepository userRepository;

    /**
     * 校验后台 token。
     * @param token 请求头或 GetInfo 查询参数；空串视为过期
     * @param touch true 表示本次是用户操作（打开页面、刷新、点击后续期），轮询必须传 false
     * @return EXPIRED 时后台 token 已清空；SKIP 表示护士站/叫号器 token，不应用 30 分钟策略
     */
    @Transactional
    public Check checkToken(String token, boolean touch) {
        if (token == null || token.isBlank()) {
            return Check.EXPIRED;
        }
        Users user = userRepository.findByToken(token).orElse(null);
        if (user == null) {
            return Check.EXPIRED;
        }
        if (!isAdminAccount(user)) {
            return Check.SKIP;
        }
        long idle = idleMillis(user.getTokenTime());
        if (idle >= IDLE_MILLIS) {
            log.info("后台会话过期: userNo={} idleMs={}", user.getUserNo(), idle);
            user.setToken("");
            user.setTokenTime(null);
            userRepository.save(user);
            return Check.EXPIRED;
        }
        if (touch && idle >= RENEW_GAP_MILLIS) {
            user.setTokenTime(new Date());
            userRepository.save(user);
        }
        return Check.OK;
    }

    /** 空登录场景与登录校验一致，视为后台账号 */
    public boolean isAdminAccount(Users user) {
        LoginScene scene = LoginScene.fromCode(user.getLoginScene());
        return scene == null || scene == LoginScene.ADMIN;
    }

    private static long idleMillis(Date tokenTime) {
        if (tokenTime == null) {
            return IDLE_MILLIS;
        }
        return System.currentTimeMillis() - tokenTime.getTime();
    }
}
