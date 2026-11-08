package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.dto.SysConfigInput;
import com.gocent.pdjh.entity.SysConfig;
import com.gocent.pdjh.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/SystemConfig")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SysConfigService sysConfigService;

    /**
     * 获取所有系统配置列表 (后台管理使用)
     */
    @GetMapping("/GetAllConfigs")
    public ResultData<List<SysConfig>> getAllConfigs() {
        List<SysConfig> data = sysConfigService.getAllConfigs();
        return ResultData.success(data, "获取成功");
    }

    /**
     * 按配置键查询单条（兼容旧前端 getByKey 调用）
     */
    @GetMapping({ "/GetConfigByKey", "/getByKey" })
    public ResultData<SysConfig> getConfigByKey(@RequestParam("key") String key) {
        if (key == null || key.isBlank()) {
            return ResultData.fail(400, "配置键不能为空");
        }
        SysConfig config = sysConfigService.getConfigByKey(key.trim());
        return config != null
                ? ResultData.success(config, "获取成功")
                : ResultData.fail(404, "未找到配置项: " + key);
    }

    /**
     * 保存或更新单个配置项。RULE_* 越界/非法 JSON 返回 400，不写库。
     */
    @PostMapping("/SaveConfig")
    public ResultData<Boolean> saveConfig(@RequestBody SysConfigInput input) {
        if (input == null || input.getConfigKey() == null || input.getConfigKey().isBlank()) {
            return ResultData.fail(500, "保存失败，配置键不能为空");
        }
        try {
            boolean success = sysConfigService.saveOrUpdateConfig(input.toEntity(), input.getOperator());
            return success ? ResultData.success(true, "保存成功") : ResultData.fail(500, "保存失败");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    /**
     * 获取客户端基础配置信息集合 (用于前端大屏和呼叫端初始化)
     */
    @GetMapping("/GetClientManifest")
    public ResultData<Map<String, String>> getClientManifest() {
        List<SysConfig> configs = sysConfigService.getAllConfigs();
        Map<String, String> dict = configs.stream()
                .filter(c -> c.getConfigKey() != null)
                .collect(Collectors.toMap(SysConfig::getConfigKey, SysConfig::getConfigValue, (v1, v2) -> v2));

        return ResultData.success(dict, "获取成功");
    }

    /**
     * 删除系统配置 
     */
    @PostMapping("/DeleteConfig")
    public ResultData<Boolean> deleteConfig(@RequestBody Map<String, String> req) {
        String configKey = req.get("ConfigKey");
        if (configKey == null || configKey.isBlank()) {
            configKey = req.get("configKey");
        }
        boolean success = sysConfigService.deleteConfig(configKey != null ? configKey.trim() : null);
        return success ? ResultData.success(true, "删除成功") : ResultData.fail(500, "删除失败");
    }
}
