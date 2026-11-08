package com.gocent.pdjh.service.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gocent.pdjh.constant.TerminalConstants;
import com.gocent.pdjh.entity.SysConfig;
import com.gocent.pdjh.service.SysConfigService;

import java.util.HashMap;
import java.util.Map;

/** 门边屏 CLINIC_CFG_{screenId} 解析与默认值 */
public final class ClinicDisplayConfigSupport {

    private ClinicDisplayConfigSupport() {}

    public static Map<String, Object> load(SysConfigService sysConfigService, ObjectMapper objectMapper, String screenId) {
        Map<String, Object> defaults = defaults();
        if (screenId == null || screenId.isBlank()) {
            return defaults;
        }
        SysConfig row = sysConfigService.getConfigByKey(TerminalConstants.CLINIC_CFG_PREFIX + screenId);
        if (row == null || row.getConfigValue() == null || row.getConfigValue().isBlank()) {
            return defaults;
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(
                    row.getConfigValue(), new TypeReference<Map<String, Object>>() {});
            defaults.putAll(parsed);
            return defaults;
        } catch (Exception e) {
            return defaults;
        }
    }

    public static Map<String, Object> defaults() {
        Map<String, Object> m = new HashMap<>();
        m.put("nameDesensitize", true);
        m.put("showWaitList", true);
        m.put("scrollIntervalSec", 5);
        return m;
    }

    public static boolean nameDesensitize(Map<String, Object> cfg) {
        Object v = cfg.get("nameDesensitize");
        return v == null || Boolean.TRUE.equals(v) || "true".equals(String.valueOf(v));
    }

    public static boolean showWaitList(Map<String, Object> cfg) {
        Object v = cfg.get("showWaitList");
        return v == null || Boolean.TRUE.equals(v) || "true".equals(String.valueOf(v));
    }

    public static int scrollIntervalSec(Map<String, Object> cfg, Integer screenScrollTime) {
        Object v = cfg.get("scrollIntervalSec");
        if (v instanceof Number) {
            int n = ((Number) v).intValue();
            return n > 0 ? n : 5;
        }
        if (screenScrollTime != null && screenScrollTime > 0) {
            return screenScrollTime;
        }
        return 5;
    }
}
