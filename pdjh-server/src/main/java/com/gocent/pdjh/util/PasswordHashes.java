package com.gocent.pdjh.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 用户表密码：BCrypt 加密后入库，明文不落库。
 * 消费者：{@code UserServiceImpl} 登录/建用户/改密，{@code AdminBootstrapRunner} 默认超管。
 */
public final class PasswordHashes {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordHashes() {
    }

    /**
     * 将明文加密为 BCrypt；已是哈希则原样返回，避免二次加密。
     * @param raw 登录明文或已有哈希；空值原样返回
     */
    public static String hash(String raw) {
        if (raw == null || raw.isBlank() || isHashed(raw)) {
            return raw;
        }
        return ENCODER.encode(raw);
    }

    /**
     * 校验登录明文是否匹配库中存储值。
     * 已哈希走 BCrypt；遗留明文做一次性相等比较（随后应由调用方改写为哈希）。
     */
    public static boolean matches(String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        if (isHashed(stored)) {
            return ENCODER.matches(raw, stored);
        }
        return stored.equals(raw);
    }

    /** BCrypt 串形如 $2a$10$...，长度约 60 */
    public static boolean isHashed(String stored) {
        return stored != null && stored.length() >= 60 && stored.startsWith("$2");
    }
}
