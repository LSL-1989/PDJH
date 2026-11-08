package com.gocent.pdjh.exception;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.service.AdminSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleGlobalException(Exception e) {
        log.error("【系统全局异常拦截】捕获到未处理异常:", e);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        result.put("success", false);
        result.put("msg", e.getMessage());
        result.put("message", e.getMessage());
        // 生产环境不建议抛出底层错误信息
        // result.put("errorDetail", e.getMessage()); 
        return result;
    }
    
    @ExceptionHandler(TerminalBindingException.class)
    public ResultData<Void> handleTerminalBinding(TerminalBindingException ex) {
        return ResultData.fail(ex.getCode(), ex.getMessage());
    }

    /** 后台空闲超时：HTTP 200 + code 50014，前端拦截器统一回登录页 */
    @ExceptionHandler(SessionExpiredException.class)
    public ResultData<Void> handleSessionExpired(SessionExpiredException ex) {
        return ResultData.fail(AdminSessionService.EXPIRED_CODE, ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        log.warn("【客户端参数异常】缺少请求参数: {}", e.getParameterName());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 400);
        result.put("success", false);
        result.put("msg", "缺少参数: " + e.getParameterName());
        result.put("message", e.getMessage());
        return result;
    }

    /**
     * 业务参数非法：走 ResultData code=400（HTTP 仍 200），
     * 与前端 axios「code===400 软失败、由调用方提示」约定一致；勿再标 BAD_REQUEST，否则会被当成网络 400。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResultData<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("【客户端参数异常】: {}", e.getMessage());
        return ResultData.fail(400, e.getMessage());
    }
}
