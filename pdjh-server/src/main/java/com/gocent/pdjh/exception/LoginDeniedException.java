package com.gocent.pdjh.exception;

/**
 * 登录被拒绝（账号、角色或权限配置不满足要求）
 */
public class LoginDeniedException extends RuntimeException {

    public LoginDeniedException(String message) {
        super(message);
    }
}
