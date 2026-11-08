package com.gocent.pdjh.exception;

/** 管理后台登录态已失效（无操作超时或 token 已清除） */
public class SessionExpiredException extends RuntimeException {

    public SessionExpiredException(String message) {
        super(message);
    }
}
