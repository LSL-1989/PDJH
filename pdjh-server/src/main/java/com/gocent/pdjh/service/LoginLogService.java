package com.gocent.pdjh.service;

/**
 * 写入登录审计。与 sys_action_log 分离：本表只记认证成败，不记业务操作。
 * 消费者：UserServiceImpl、DoctorStationServiceImpl。
 */
public interface LoginLogService {

    /**
     * 记录一次登录尝试。写库失败不影响登录主流程。
     * @param userNo 工号/帐号；可为空
     * @param scene 登录场景码
     * @param success 是否通过校验
     * @param failReason 失败原因；成功时传 null
     */
    void record(String userNo, String scene, boolean success, String failReason);
}
