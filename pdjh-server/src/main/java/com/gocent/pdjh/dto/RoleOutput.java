package com.gocent.pdjh.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色输出DTO（与 Element 表格 PascalCase 列一致）
 */
@Data
public class RoleOutput {
    @JsonProperty("RoleId")
    private String roleId;

    @JsonProperty("RoleName")
    private String roleName;

    @JsonProperty("RoleCode")
    private String roleCode;

    @JsonProperty("IsDelete")
    private Boolean isDelete;

    @JsonProperty("IsDeleteText")
    private String isDeleteText;

    @JsonProperty("CreateDateTime")
    private LocalDateTime createDateTime;

    @JsonProperty("Remark")
    private String remark;
}
