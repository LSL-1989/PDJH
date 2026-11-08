package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.ScreensOutput;
import com.gocent.pdjh.entity.DocScreen;

/** 屏幕配置生命周期：待配置 / 启用 / 冻结，及终端连接心跳 */
public interface ScreenLifecycleService {

    /** 绑定是否满足终端 Resolve READY 条件 */
    boolean isBindingComplete(String screenId, String terminalType);

    /** 名称、呼叫代码、设备编号、诊室绑定等是否齐全 */
    boolean isConfigComplete(DocScreen screen);

    /**
     * 应用生命周期：FREEZE 冻结；UNFREEZE 解除后按配置推断；null 且非冻结时自动同步。
     */
    void applyLifecycle(DocScreen screen, String lifecycleAction);

    /** 绑定变更后刷新（非冻结设备） */
    void refreshAfterBinding(String screenId);

    /** 终端 Register/Resolve 时更新 lastSeenAt */
    void touchLastSeen(String screenId);

    /** 最近 5 分钟内有过终端请求视为已连接 */
    boolean isOnline(DocScreen screen);

    void enrichOutput(ScreensOutput output, DocScreen screen);
}
