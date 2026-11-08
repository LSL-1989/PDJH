package com.gocent.pdjh.enums;

/**
 * 用户登录场景：账户可在对应客户端登录（§23.8 已决策）。
 * 消费者：Users.loginScene、用户管理页筛选、各端登录校验。
 */
public enum LoginScene {
    ADMIN("后台管理"),
    DOCTOR("叫号器（门诊）"),
    NURSE("护士站"),
    PHARMACY("叫号器（药房）"),
    LAB("叫号器（检验）"),
    EXAM("叫号器（医技）");

    private final String label;

    LoginScene(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getCode() {
        return name();
    }

    /** 叫号器 WPF：门诊/药房/检验/医技窗口岗位 */
    public boolean isCallStation() {
        return this == DOCTOR || this == PHARMACY || this == LAB || this == EXAM;
    }

    /** 管理后台 Web */
    public boolean isAdminWeb() {
        return this == ADMIN;
    }

    /** 护士站 Web */
    public boolean isNurseWeb() {
        return this == NURSE;
    }

    /** 校验场景码；非法或不存在返回 null */
    public static LoginScene fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return LoginScene.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }
}
