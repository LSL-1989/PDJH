package com.gocent.pdjh.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 新增/修改用户入参（与 Vue 管理端字段名一致）
 */
@Data
public class UserInput {
    @JsonProperty("UserNo")
    private String userNo;

    @JsonProperty("UserName")
    private String userName;

    /** 新增时可空，空则默认 123456 */
    @JsonProperty("PassWord")
    private String passWord;

    @JsonProperty("Phone")
    private String phone;

    /** 登录场景：ADMIN/DOCTOR/NURSE/PHARMACY/LAB/EXAM，必填 */
    @JsonProperty("LoginScene")
    private String loginScene;

    @JsonProperty("State")
    private Integer state;

    @JsonProperty("DeptID")
    private String deptId;

    @JsonProperty("Role")
    private String role;

    @JsonProperty("RoleId")
    private String roleId;

    @JsonProperty("SignCode")
    private String signCode;
}
