package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.DoctorLoginVO;

/**
 * 叫号器登录与密码：登录走 users（BCrypt），改密/重置写 users；Doctor/Login 为兼容别名。
 * 申请重置会同步写入统一 users 表，供后台「用户管理」处理
 */
public interface DoctorStationService {

    /**
     * 工号或手机号 + 密码登录；成功返回档案与默认/可选诊室
     * @param account 工号或手机号
     * @param password 密码
     */
    DoctorLoginVO login(String account, String password);

    /**
     * 医生修改密码；成功后清除 mustChangePassword 与申请标记
     * @param doctorId 医生 id
     * @param oldPassword 原密码；强制改密场景可传与当前密码相同或空（后端按 mustChange 放宽）
     * @param newPassword 新密码
     */
    void changePassword(String doctorId, String oldPassword, String newPassword);

    /**
     * 忘记密码：按工号/手机号登记申请；优先写 users，医生档案同步标记；后台在用户管理处理
     * @param account 工号或手机号
     */
    void requestPasswordReset(String account);

    /**
     * 管理员重置密码为默认值并强制下次登录改密；同时清除申请标记
     * @param doctorId 医生 id
     */
    void adminResetPassword(String doctorId);

    /**
     * 坐诊开诊：将医生绑定到所选诊室（写 office_room.doctor_id），
     * 供门边屏展示；并解除该医生在其他诊室的坐诊绑定。
     * 成功后广播 STATE_UPDATE，门边屏及时刷新坐诊医生。
     * @param doctorId 医生 id
     * @param officeRoomId 诊室 id
     * @return 绑定后的诊室
     */
    com.gocent.pdjh.entity.OfficeRoom bindSittingRoom(String doctorId, String officeRoomId);

    /**
     * 退出工作台离岗：清除该医生所有 office_room.doctor_id，isOnDuty=0，
     * 门边屏变为「暂无值班」；与 BindSittingRoom 成对，不可用 User/LogOut 替代。
     * @param doctorId 医生 id（必填）
     * @param officeRoomId 当前坐诊诊室 id（可选；再清该诊室，防止 findByDoctorId 漏清）
     */
    void leaveSittingRoom(String doctorId, String officeRoomId);

    /**
     * 专家临时停诊：离诊 + 当日排班 OFF + 广播 DOCTOR_SUSPEND
     * @param doctorId 医生 id
     * @param remark 停诊原因
     */
    void suspendDuty(String doctorId, String remark);

    /**
     * 主任台取消临时停诊：清除当日 OFF 排班；医生需重新登录叫号器开诊。
     * @param doctorId 医生 id
     */
    void resumeDuty(String doctorId);

    /**
     * 挂号科室下当前已登录叫号器的坐诊医生（office_room.doctor_id + isOnDuty=1）
     */
    java.util.List<com.gocent.pdjh.dto.SittingDoctorVO> listSittingDoctorsByDept(String deptId);
}
