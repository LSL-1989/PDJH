package com.gocent.pdjh.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 角色输入DTO（与后台 Element 表单字段 PascalCase 对齐）
 */
@Data
public class RoleInput {
    @JsonProperty("RoleId")
    private String roleId;

    @JsonProperty("RoleName")
    private String roleName;

    @JsonProperty("RoleCode")
    private String roleCode;

    /** 前端 el-radio 提交 "0"=启用 "1"=禁用 */
    @JsonProperty("IsDelete")
    private String isDelete;

    @JsonProperty("Remark")
    private String remark;

    /** 是否禁用：true=禁用，false=启用 */
    public boolean isDisabledFlag() {
        if (isDelete == null || isDelete.isEmpty()) {
            return false;
        }
        return "1".equals(isDelete) || "true".equalsIgnoreCase(isDelete);
    }
}
