package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.SignDeviceOutput;
import com.gocent.pdjh.entity.SignDevice;

/** sign_device 配置状态与终端连接状态（last_seen_at） */
public interface SignLifecycleService {

    /** 是否已绑定至少一个物理诊室 */
    boolean isBindingComplete(String signDeviceId);

    /** 配置是否完整（当前与诊室绑定一致） */
    boolean isConfigComplete(SignDevice device);

    /**
     * 应用生命周期：DISABLE 停用 | ENABLE 解除停用后按配置推断；null 且非停用时自动同步。
     */
    void applyLifecycle(SignDevice device, String lifecycleAction);

    /** 诊室绑定变更后刷新配置状态 */
    void refreshAfterBinding(String signDeviceId);

    /** 终端 Register/Resolve/GetDeviceConfig 时更新 lastSeenAt */
    void touchLastSeen(String signDeviceId);

    boolean isOnline(SignDevice device);

    void enrichOutput(SignDeviceOutput output, SignDevice device);
}
