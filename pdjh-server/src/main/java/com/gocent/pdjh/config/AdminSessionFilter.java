package com.gocent.pdjh.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.service.AdminSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 管理后台接口会话闸门：携带 X-Token 的请求在过期时直接返回 50014，前端据此回登录页。
 * 无此头的护士站、叫号器、签到机、大屏请求原样放行。
 * 登录、登出不校验，避免过期 token 挡住重新登录。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class AdminSessionFilter extends OncePerRequestFilter {

    private static final Set<String> SKIP_PATHS = Set.of(
            "/User/LoginUser",
            "/User/GetUserInfo",
            "/User/LogOut"
    );

    private final AdminSessionService adminSessionService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = request.getHeader("X-Token");
        if (token == null || token.isBlank() || SKIP_PATHS.contains(servletPath(request))) {
            filterChain.doFilter(request, response);
            return;
        }
        boolean touch = "1".equals(request.getHeader("X-Touch"));
        AdminSessionService.Check check = adminSessionService.checkToken(token, touch);
        if (check == AdminSessionService.Check.EXPIRED) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            objectMapper.writeValue(response.getWriter(),
                    ResultData.fail(AdminSessionService.EXPIRED_CODE, AdminSessionService.EXPIRED_MSG));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static String servletPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && uri.startsWith(context)) {
            uri = uri.substring(context.length());
        }
        int query = uri.indexOf('?');
        return query >= 0 ? uri.substring(0, query) : uri;
    }
}
