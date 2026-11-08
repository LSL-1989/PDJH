package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.PasswordChangeInput;
import com.gocent.pdjh.dto.TokenInput;
import com.gocent.pdjh.dto.TokenOutput;
import com.gocent.pdjh.dto.UserInput;
import com.gocent.pdjh.entity.Users;

import java.util.List;
import java.util.Map;

public interface UserService {

    // 登陆系统，返回 Token
    TokenOutput loginUser(TokenInput input);

    // 根据 Token 获取登陆用户信息
    TokenOutput getInfo(String token);

    // 登出清理 Token
    boolean logOut(String userNo);

    // 根据秘钥直接获取信息（兼容 C# 里的 GetUserInfo）
    TokenOutput getUserInfo(TokenInput input);

    // 新增用户
    boolean addUser(UserInput input);

    // 更新用户
    boolean editorUser(UserInput input);

    // 删除用户
    boolean removeUser(String userNo);

    /**
     * 分页用户列表：data + pageInfo.totalCount
     * @param keyword 匹配帐号/工号、用户名、手机号
     * @param roleId 角色筛选
     * @param loginScene 登录场景筛选（ADMIN/DOCTOR/NURSE/…）
     * @param passwordResetRequested 1=仅待重置申请
     */
    java.util.Map<String, Object> getUsers(int pageIndex, int pageSize, String keyword, String roleId,
                                           String loginScene, Integer passwordResetRequested);

    Map<String, String> getUserClinicDept(String userNo);

    List<Map<String, String>> getPatientByCardNo(String patientCardNo);

    /** 用户自己改密（需原密码） */
    boolean resetPassword(PasswordChangeInput input);

    /**
     * 忘记密码：按帐号/工号或手机号登记重置申请，供用户管理提示管理员处理
     * @param account 帐号/工号或手机号
     */
    void requestPasswordReset(String account);

    /**
     * 管理员重置为默认密码 123456 并强制下次改密；同时清除申请标记
     * @param userNo 帐号/工号
     */
    void adminResetPassword(String userNo);
}
