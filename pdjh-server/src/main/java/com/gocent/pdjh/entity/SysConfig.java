package com.gocent.pdjh.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sys_config")
public class SysConfig {

    @Id
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String configKey;

    /** 配置值（JSON 等）；大屏 SCREEN_CFG_* 默认模板约 400+ 字符，需 TEXT */
    @Column(columnDefinition = "TEXT")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String configValue;

    @Column(columnDefinition = "TEXT")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String description;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String tenantId;

    @JsonProperty("ConfigKey")
    public String getConfigKey() {
        return configKey;
    }

    @JsonProperty("ConfigKey")
    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    @JsonProperty("ConfigValue")
    public String getConfigValue() {
        return configValue;
    }

    @JsonProperty("ConfigValue")
    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    @JsonProperty("Description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("Description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("TenantId")
    public String getTenantId() {
        return tenantId;
    }

    @JsonProperty("TenantId")
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
