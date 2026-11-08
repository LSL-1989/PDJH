package com.gocent.pdjh.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gocent.pdjh.entity.SysConfig;
import lombok.Data;

/** 保存/更新系统配置请求体（兼容前端 PascalCase 与 camelCase） */
@Data
public class SysConfigInput {

    @JsonProperty("ConfigKey")
    @JsonAlias("configKey")
    private String configKey;

    @JsonProperty("ConfigValue")
    @JsonAlias("configValue")
    private String configValue;

    @JsonProperty("Description")
    @JsonAlias("description")
    private String description;

    @JsonProperty("TenantId")
    @JsonAlias("tenantId")
    private String tenantId;

    /** 规则中心保存时的操作人，写入 RULE_CHANGE 审计；非 RULE_* 可空 */
    @JsonProperty("Operator")
    @JsonAlias("operator")
    private String operator;

    public SysConfig toEntity() {
        SysConfig entity = new SysConfig();
        entity.setConfigKey(configKey != null ? configKey.trim() : null);
        entity.setConfigValue(configValue);
        entity.setDescription(description);
        entity.setTenantId(tenantId != null && !tenantId.isBlank() ? tenantId.trim() : null);
        return entity;
    }
}
