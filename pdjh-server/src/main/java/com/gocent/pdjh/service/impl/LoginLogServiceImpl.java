package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.entity.LoginLog;
import com.gocent.pdjh.repository.LoginLogRepository;
import com.gocent.pdjh.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 登录审计落库。IP / UA 从当前 HTTP 请求读取（无请求上下文时留空）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {

    private final LoginLogRepository loginLogRepository;

    @Override
    public void record(String userNo, String scene, boolean success, String failReason) {
        try {
            LoginLog row = new LoginLog();
            row.setUserNo(userNo);
            row.setScene(scene);
            row.setSuccess(success ? 1 : 0);
            row.setFailReason(success ? null : failReason);
            HttpServletRequest req = currentRequest();
            if (req != null) {
                row.setIp(clientIp(req));
                String ua = req.getHeader("User-Agent");
                if (ua != null && ua.length() > 500) {
                    ua = ua.substring(0, 500);
                }
                row.setUserAgent(ua);
            }
            loginLogRepository.save(row);
        } catch (Exception e) {
            log.warn("写入 login_log 失败: {}", e.getMessage());
        }
    }

    private static HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }

    private static String clientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
