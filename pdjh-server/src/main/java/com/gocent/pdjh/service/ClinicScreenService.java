package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.clinic.ClinicDisplayVO;

public interface ClinicScreenService {
    /** 现场门边屏拉取展示数据，并刷新 lastSeenAt */
    ClinicDisplayVO buildDisplay(String deviceCode);

    /**
     * 与 {@link #buildDisplay(String)} 同一份展示数据。
     * preview 为 true 时不写 lastSeenAt，供管理后台只读预览，避免把预览窗口算成现场在线。
     */
    ClinicDisplayVO buildDisplay(String deviceCode, boolean preview);
}
