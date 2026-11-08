package com.gocent.pdjh.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class TokenOutput {
    @JsonProperty("UserNo")
    private String userNo;

    @JsonProperty("UserName")
    private String userName;

    @JsonProperty("DeptCode")
    private String deptCode;

    @JsonProperty("DeptId")
    private String deptId;

    @JsonProperty("DeptName")
    private String deptName;

    @JsonProperty("Token")
    private String token;

    @JsonProperty("ValidPeriod")
    private Date validPeriod;

    @JsonProperty("Role")
    private String role;

    @JsonProperty("RoleId")
    private String roleId;

    @JsonProperty("RoleName")
    private String roleName;

    @JsonProperty("SignCode")
    private String signCode;

    @JsonProperty("Routes")
    private List<Object> routes = new ArrayList<>();

    @JsonProperty("ClientType")
    private String clientType;
}
