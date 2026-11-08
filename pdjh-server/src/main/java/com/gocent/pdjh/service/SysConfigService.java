package com.gocent.pdjh.service;

import com.gocent.pdjh.entity.SysConfig;
import com.gocent.pdjh.repository.SysConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置读写。RULE_* 保存时做 JSON 边界校验并写 RULE_CHANGE 审计。
 * 消费者：规则中心、全局字典、各调度引擎读配置。
 */
@Service
@RequiredArgsConstructor
public class SysConfigService {

    private final SysConfigRepository sysConfigRepository;
    private final SystemActionLogService systemActionLogService;

    public List<SysConfig> getAllConfigs() {
        return sysConfigRepository.findAll().stream()
                .sorted((a, b) -> {
                    String ka = a.getConfigKey() != null ? a.getConfigKey() : "";
                    String kb = b.getConfigKey() != null ? b.getConfigKey() : "";
                    return ka.compareToIgnoreCase(kb);
                })
                .toList();
    }

    public SysConfig getConfigByKey(String configKey) {
        return sysConfigRepository.findById(configKey).orElse(null);
    }

    public boolean saveOrUpdateConfig(SysConfig config) {
        return saveOrUpdateConfig(config, null);
    }

    /**
     * 保存配置。RULE_* 非法 JSON/越界抛 IllegalArgumentException，由 Controller 转 400。
     * @param operator 操作人显示名；可空，审计记为 SYS
     */
    @Transactional
    public boolean saveOrUpdateConfig(SysConfig config, String operator) {
        if (config == null || config.getConfigKey() == null || config.getConfigKey().trim().isEmpty()) {
            return false;
        }
        String key = config.getConfigKey().trim();
        config.setConfigKey(key);
        if (key.startsWith("RULE_")) {
            List<String> errors = RuleConfigValidator.validate(key, config.getConfigValue());
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException(String.join("；", errors));
            }
        }
        SysConfig previous = sysConfigRepository.findById(key).orElse(null);
        String before = previous != null ? previous.getConfigValue() : null;
        sysConfigRepository.save(config);
        if (key.startsWith("RULE_")) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("configKey", key);
            payload.put("before", before);
            payload.put("after", config.getConfigValue());
            systemActionLogService.recordLogAsync(
                    "RULE_CHANGE",
                    operator != null && !operator.isBlank() ? operator.trim() : "SYS",
                    "修改规则 " + key,
                    payload,
                    true,
                    null);
        }
        return true;
    }

    public boolean deleteConfig(String configKey) {
        if (!sysConfigRepository.existsById(configKey)) {
            return false;
        }
        sysConfigRepository.deleteById(configKey);
        return true;
    }
}
