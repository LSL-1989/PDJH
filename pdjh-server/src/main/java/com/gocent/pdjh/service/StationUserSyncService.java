package com.gocent.pdjh.service;

import com.gocent.pdjh.entity.Doctor;
import com.gocent.pdjh.entity.OfficeRoom;
import com.gocent.pdjh.entity.Users;

/**
 * 叫号器岗位账号与 users 对齐（已决策：登录只走 users、单一 BCrypt、doctor.password 废弃）。
 * 消费者：DoctorStationServiceImpl 登录/改密、DoctorServiceImpl 保存档案。
 * 与 UserService.addUser 的差异：医生档案自动开户；窗口岗位仅有 users 时补最小 doctor 供绑窗。
 */
public interface StationUserSyncService {

    /**
     * 确保医生有对应 users 记录（工号同人同号），并把 doctor.password 迁入 BCrypt 后清空。
     * @param doctor 已持久化或即将保存的医生档案
     * @return 对齐后的 users；工号为空时抛 IllegalArgumentException
     */
    Users ensureUser(Doctor doctor);

    /**
     * 按科室/诊室 businessType 得到叫号器 stationMode：CLINIC / PHARMACY / LAB / EXAM。
     * 空值按 CLINIC。
     */
    String stationModeFromBusinessType(String businessType);

    /**
     * 上岗窗口优先：诊室 businessType，缺省再查科室。
     */
    String stationModeFromRoom(OfficeRoom room);

    /**
     * 新建 users 时的 login_scene：PHARMACY / LAB / EXAM / DOCTOR。
     * 已有 ADMIN 帐号不覆盖。
     */
    String loginSceneFromBusinessType(String businessType);

    /**
     * 按工号/手机号找 users（叫号器登录优先入口）。
     */
    Users findUserByAccount(String account);

    /**
     * 按 user_no / work_no / 手机号找已有医生档案；没有则返回 empty。
     */
    java.util.Optional<Doctor> findLinkedDoctor(Users user);

    /**
     * 窗口岗位只有 users、没有 doctor 档案时补一条最小档案，供上岗绑窗。
     * 已有档案则只补 user_no 关联，不覆盖职称/专长。
     */
    Doctor ensureDoctor(Users user);
}
