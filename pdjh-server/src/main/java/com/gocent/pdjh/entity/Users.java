package com.gocent.pdjh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Date;

/**
 * 统一登录账户：所有人员（医生/护士/药房/检验/医技/后台）的唯一登录信息源。
 * 角色扩展业务信息另存（如 doctor 档案）；本表管账号、BCrypt 密码、场景与联系方式。
 */
@Data
@Entity
@Table(name = "users")
public class Users {

    /** 新建用户默认初始密码 */
    public static final String DEFAULT_PASSWORD = "123456";

    @Id
    @Column(name = "user_no", length = 50)
    private String userNo;

    @Column(name = "user_name", length = 50)
    private String userName;

    /** BCrypt 哈希，禁止存明文；列名保持历史拼写 pass_word */
    @Column(name = "pass_word", length = 255)
    private String passWord;

    /** 手机号（可选，可用于登录） */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 登录场景码：ADMIN/DOCTOR/NURSE/PHARMACY/LAB/EXAM，见 {@link com.gocent.pdjh.enums.LoginScene}
     */
    @Column(name = "login_scene", length = 20)
    private String loginScene;

    /**
     * 是否必须修改密码：1=首次登录或管理员重置后；0=已改过
     */
    @Column(name = "must_change_password")
    private Integer mustChangePassword = 0;

    /**
     * 是否已申请重置密码：1=用户点了忘记密码，待管理员在用户管理中处理
     */
    @Column(name = "password_reset_requested")
    private Integer passwordResetRequested = 0;

    /** 忘记密码申请时间 */
    @Column(name = "password_reset_request_time")
    private Date passwordResetRequestTime;

    @Column(name = "create_date")
    private Date createDate;

    private Integer state;

    @Column(name = "dept_id", length = 36)
    private String deptID;

    @Column(length = 500)
    private String token;

    @Column(name = "token_time")
    private Date tokenTime;

    @Column(length = 50)
    private String role;

    @Column(name = "sign_code", length = 50)
    private String signCode;

    @Column(name = "role_id", length = 36) // C# 是 Guid
    private String roleId;
}
