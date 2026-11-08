package com.gocent.pdjh.constant;

/** 终端类型与状态常量（doc_screen.state / sign_device.status） */
public final class TerminalConstants {

    private TerminalConstants() {}

    public static final String TYPE_SCREEN = "SCREEN";
    public static final String TYPE_CLINIC = "CLINIC";
    public static final String TYPE_CHECKIN = "CHECKIN";

    public static final String STATUS_NOT_REGISTERED = "NOT_REGISTERED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_FROZEN = "FROZEN";
    public static final String STATUS_READY = "READY";

    public static final int SCREEN_STATE_FROZEN = 0;
    public static final int SCREEN_STATE_ACTIVE = 1;
    public static final int SCREEN_STATE_PENDING = 2;

    public static final int SIGN_STATUS_DISABLED = 0;
    public static final int SIGN_STATUS_ACTIVE = 1;
    public static final int SIGN_STATUS_PENDING = 2;

    /** 候诊区大屏（SCREEN）默认模板路由；CLINIC 门边屏不使用 pdjh 模板 */
    public static final String DEFAULT_TEMPLATE_ROUTE = "/default";

    /** 门边屏显示配置 sys_config 键前缀：CLINIC_CFG_{screenId} */
    public static final String CLINIC_CFG_PREFIX = "CLINIC_CFG_";

    /** 候诊区大屏显示配置 sys_config 键前缀：SCREEN_CFG_{screenId} */
    public static final String SCREEN_CFG_PREFIX = "SCREEN_CFG_";

    public static boolean isClinicType(String terminalType) {
        return TYPE_CLINIC.equals(terminalType);
    }

    public static boolean isScreenType(String terminalType) {
        return TYPE_SCREEN.equals(terminalType) || TYPE_CLINIC.equals(terminalType);
    }

    public static boolean isValidTerminalType(String terminalType) {
        return TYPE_SCREEN.equals(terminalType)
                || TYPE_CLINIC.equals(terminalType)
                || TYPE_CHECKIN.equals(terminalType);
    }
}
