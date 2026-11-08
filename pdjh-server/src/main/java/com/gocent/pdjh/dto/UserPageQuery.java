package com.gocent.pdjh.dto;

import lombok.Data;

/**
 * 用户分页查询：账号/用户名/手机号 keyword、角色 roleId、登录场景 loginScene
 * 消费者：UserController.GetUsers、HJ后台 maintenanceUser
 */
@Data
public class UserPageQuery {
    private Integer pageindex = 1;
    private Integer pagesize = 20;
    /** 模糊匹配 user_no / user_name / phone */
    private String keyword;
    /** 角色 ID 精确筛选 */
    private String roleId;
    /** 登录场景码精确筛选：ADMIN/DOCTOR/NURSE/PHARMACY/LAB/EXAM */
    private String loginScene;
    /** 1=仅看已申请重置密码的用户 */
    private Integer passwordResetRequested;
}
