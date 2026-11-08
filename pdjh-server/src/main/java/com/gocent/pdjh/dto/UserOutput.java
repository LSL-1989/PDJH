package com.gocent.pdjh.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

/** 用户列表/详情输出（与 Vue 管理端字段名一致，不包含密码） */
@Data
public class UserOutput {
    @JsonProperty("UserNo")
    private String userNo;

    @JsonProperty("UserName")
    private String userName;

    @JsonProperty("Phone")
    private String phone;

    /** 登录场景码 */
    @JsonProperty("LoginScene")
    private String loginScene;

    /** 登录场景中文名，便于列表展示 */
    @JsonProperty("LoginSceneName")
    private String loginSceneName;

    /** 1=已申请重置密码，待管理员处理 */
    @JsonProperty("PasswordResetRequested")
    private Integer passwordResetRequested;

    @JsonProperty("PasswordResetRequestTime")
    private Date passwordResetRequestTime;

    @JsonProperty("CreateDate")
    private Date createDate;

    @JsonProperty("State")
    private Integer state;

    @JsonProperty("DeptID")
    private String deptID;

    @JsonProperty("Role")
    private String role;

    @JsonProperty("RoleId")
    private String roleId;

    @JsonProperty("RoleName")
    private String roleName;

    @JsonProperty("SignCode")
    private String signCode;
}
