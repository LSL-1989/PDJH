package com.gocent.pdjh.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 医生业务档案（职称/专长/排班/诊室）。登录与密码已决策收敛到 users，本表不再承担认证。
 * 消费者：排班、门边屏展示、叫号器坐诊绑定；认证走 Doctor/Login → users。
 */
@Data
@Entity
@Table(name = "doctor")
public class Doctor {

    /** 兼容常量：新建 users 时的默认明文，入库前由 StationUserSyncService 做 BCrypt */
    public static final String DEFAULT_PASSWORD = Users.DEFAULT_PASSWORD;

    @Id
    @Column(length = 64)
    private String id;

    /** 工号 */
    @Column(name = "work_no", length = 50)
    private String workNo;

    /** 关联 users.user_no（同人同号）；迁移完成后为唯一登录映射键 */
    @Column(name = "user_no", length = 50)
    private String userNo;

    /** 姓名 */
    @Column(length = 50)
    private String name;

    /** 性别 */
    @Column(length = 10)
    private String gender;

    /** 手机号（叫号器可用手机号登录） */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 已废弃：登录密码改存 users.pass_word（BCrypt）。列保留供迁移读取，写入后应清空。
     */
    @JsonIgnore
    @Column(name = "password", length = 100)
    private String password;

    /**
     * 是否必须修改密码：1=首次登录或管理员重置后；0=已改过
     */
    @Column(name = "must_change_password")
    private Integer mustChangePassword = 1;

    /**
     * 是否已申请重置密码：1=医生点了忘记密码，待管理员处理
     */
    @Column(name = "password_reset_requested")
    private Integer passwordResetRequested = 0;

    /** 忘记密码申请时间 */
    @Column(name = "password_reset_request_time")
    private Date passwordResetRequestTime;

    /** 职称名称，取值来自 dict_doctor_title.name */
    @Column(length = 50)
    private String title;

    /** 所属科室ID（保存时写入 deptIds 的第一项，兼容只读单科室的调用方） */
    @Column(name = "dept_id", length = 64)
    private String deptId;

    /**
     * 所属科室 id 列表，来自 doctor_dept。仅 GetAll 填充，不入库。
     * 排班页按此并集筛诊室；分页列表仍走 toRow 的 deptIds，不读本字段。
     */
    @Transient
    private List<String> deptIds;

    /** 专长描述 */
    @Column(length = 500)
    private String specialty;

    /** 医生简介（展示用） */
    @Column(length = 500)
    private String remarks;

    /** 展示档案状态：1=启用 0=冻结（与 is_on_duty 分离） */
    @Column(name = "display_status")
    private Integer displayStatus = 1;

    /** 当前绑定的叫号大屏 screen_id */
    @Column(name = "screen_id", length = 64)
    private String screenId;

    /** 当前诊室名称（如"内科一诊室"） */
    @Column(name = "office_name", length = 100)
    private String officeName;

    /** 是否出诊（1=出诊中 0=停诊） */
    @Column(name = "is_on_duty")
    private Integer isOnDuty = 1;

    /** 头像路径（如 /uploads/intros/xxx.png） */
    @Column(name = "avatar", length = 255)
    private String avatar;

    /** 创建时间 */
    @Column(name = "create_date")
    private Date createDate;
}
