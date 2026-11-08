package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.DoctorLoginVO;
import com.gocent.pdjh.dto.ResultData;
import com.gocent.pdjh.entity.Doctor;
import com.gocent.pdjh.entity.OfficeRoom;
import com.gocent.pdjh.exception.LoginDeniedException;
import com.gocent.pdjh.repository.DoctorRepository;
import com.gocent.pdjh.repository.OfficeRoomRepository;
import com.gocent.pdjh.service.DoctorService;
import com.gocent.pdjh.service.DoctorStationService;
import com.gocent.pdjh.service.HisSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 医生主数据接口（业务档案 + 展示信息，含原 screen_intro 能力）
 * 消费者：DoctorProfile、doctorSchedule、HIS 同步、诊室屏、HJ叫号器登录
 */
@RestController
@RequestMapping("/Doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final OfficeRoomRepository officeRoomRepository;
    private final HisSyncService hisSyncService;
    private final DoctorService doctorService;
    private final DoctorStationService doctorStationService;

    /** 全部医生。deptIds 为 doctor_dept 绑定，供排班按所属科室筛诊室；与分页列表的科室字段一致，不另开接口 */
    @PostMapping("/GetAll")
    public ResultData<List<Doctor>> getAll() {
        return ResultData.success(doctorService.listAll(), "获取成功");
    }

    /** 分页条件查询医生列表 */
    @PostMapping("/GetPageList")
    public ResultData<Map<String, Object>> getPageList(@RequestBody Map<String, Object> query) {
        return ResultData.success(doctorService.pageList(query), "查询成功");
    }

    /** 按科室获取医生 */
    @PostMapping("/GetByDept")
    public ResultData<List<Doctor>> getByDept(@RequestParam("deptId") String deptId) {
        return ResultData.success(doctorRepository.findByDeptIdOrderByTitleAsc(deptId), "获取成功");
    }

    /** 获取当前出诊医生列表（全院 isOnDuty=1，不含诊室绑定校验） */
    @PostMapping("/GetOnDuty")
    public ResultData<List<Doctor>> getOnDuty() {
        return ResultData.success(doctorRepository.findByIsOnDuty(1), "获取成功");
    }

    /**
     * 按挂号科室返回已登录叫号器、可供签到选择的坐诊医生。
     * body：{ "deptId": "..." }
     */
    @PostMapping("/GetSittingByDept")
    public ResultData<List<com.gocent.pdjh.dto.SittingDoctorVO>> getSittingByDept(
            @RequestBody(required = false) Map<String, Object> body) {
        String deptId = body != null ? str(body, "deptId", "deptID") : "";
        if (deptId == null || deptId.isBlank()) {
            return ResultData.fail(400, "deptId 不能为空");
        }
        return ResultData.success(doctorStationService.listSittingDoctorsByDept(deptId), "获取成功");
    }

    /**
     * 按工号或 doctor.id 查询；未找到返回 404（新增时用于工号重复校验）
     * POST JSON：{ "workNo": "D001" }
     */
    @PostMapping("/GetByWorkNo")
    public ResultData<Doctor> getByWorkNo(@RequestBody(required = false) Map<String, Object> body) {
        String workNo = extractWorkNo(body);
        if (workNo.isBlank()) {
            return ResultData.fail(400, "工号不能为空");
        }
        return lookupDoctor(workNo)
                .map(d -> ResultData.success(d, "获取成功"))
                .orElse(ResultData.fail(404, "未找到该工号对应的医生"));
    }

    /** 新增/更新医生（JSON body，无 id 时自动生成） */
    @PostMapping("/Save")
    public ResultData<Doctor> save(@RequestBody Map<String, Object> body) {
        try {
            return ResultData.success(doctorService.saveFromMap(body), "保存成功");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    /** 删除医生（JSON body：{ "id": "DOC001" }） */
    @PostMapping("/Delete")
    public ResultData<Boolean> delete(@RequestBody Map<String, Object> body) {
        Object idObj = body != null ? body.get("id") : null;
        if (idObj == null && body != null) {
            idObj = body.get("ID");
        }
        if (idObj == null) {
            return ResultData.fail(400, "id 不能为空");
        }
        try {
            doctorService.deleteById(idObj.toString());
            return ResultData.success(true, "删除成功");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    /** 兼容旧 query 参数删除 */
    @PostMapping("/DeleteById")
    public ResultData<Boolean> deleteByIdParam(@RequestParam("id") String id) {
        try {
            doctorService.deleteById(id);
            return ResultData.success(true, "删除成功");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    /** 上传医生头像，返回相对路径写入 doctor.avatar */
    @PostMapping(value = "/UploadAvatar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResultData<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String path = doctorService.uploadAvatar(file);
        if (path == null || path.isBlank()) {
            return ResultData.fail(500, "上传失败");
        }
        return ResultData.success(path, "上传成功");
    }

    // ── 叫号器登录 / 密码 ──────────────────────────────────

    /**
     * 叫号器登录：account 为工号或手机号 + password。
     * 兼容别名：内部校验统一 users（BCrypt），doctor 仅提供档案与诊室。
     */
    @PostMapping("/Login")
    public ResultData<DoctorLoginVO> login(@RequestBody Map<String, Object> body) {
        String account = str(body, "account", "workNo");
        String password = str(body, "password", "Password");
        try {
            return ResultData.success(doctorStationService.login(account, password), "登录成功");
        } catch (LoginDeniedException e) {
            return ResultData.fail(401, e.getMessage());
        }
    }

    /**
     * 医生修改密码（首次登录 / 重置后强制改密 / 主动改密）。
     * body：doctorId、oldPassword（强制改密时可空）、newPassword
     */
    @PostMapping("/ChangePassword")
    public ResultData<Boolean> changePassword(@RequestBody Map<String, Object> body) {
        try {
            doctorStationService.changePassword(
                    str(body, "doctorId", "id"),
                    str(body, "oldPassword", "oldPwd"),
                    str(body, "newPassword", "newPwd"));
            return ResultData.success(true, "密码已修改");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    /**
     * 忘记密码：登记重置申请；优先写入统一 users 表供「用户管理」处理，医生档案同步标记。
     * body：{ "account": "工号或手机号" }
     */
    @PostMapping("/RequestPasswordReset")
    public ResultData<Boolean> requestPasswordReset(@RequestBody Map<String, Object> body) {
        try {
            doctorStationService.requestPasswordReset(str(body, "account", "workNo"));
            return ResultData.success(true, "已提交重置申请，请联系管理员在「用户管理」中处理");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    /**
     * 管理员重置医生密码为默认值（123456）并强制下次登录改密。
     * 消费者：后台 DoctorProfile；body：{ "id": "DOC..." }
     */
    @PostMapping("/AdminResetPassword")
    public ResultData<Boolean> adminResetPassword(@RequestBody Map<String, Object> body) {
        try {
            doctorStationService.adminResetPassword(str(body, "id", "doctorId"));
            return ResultData.success(true, "已重置为默认密码，下次登录叫号器需修改");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    /**
     * 叫号器确认上岗窗口：写入 office_room.doctor_id，门边屏据此展示值班信息。
     * body：{ "doctorId", "officeRoomId" }
     */
    @PostMapping("/BindSittingRoom")
    public ResultData<OfficeRoom> bindSittingRoom(@RequestBody Map<String, Object> body) {
        try {
            OfficeRoom room = doctorStationService.bindSittingRoom(
                    str(body, "doctorId", "id"),
                    str(body, "officeRoomId", "roomId"));
            return ResultData.success(room, "窗口已确认");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    /**
     * 叫号器退出离岗：清除 office_room.doctor_id 并 isOnDuty=0，门边屏显示暂无值班。
     * 与 BindSittingRoom 成对；非 User/LogOut。
     * body：{ "doctorId"（必填）, "officeRoomId"（可选，当前上岗窗口） }
     */
    @PostMapping("/LeaveSittingRoom")
    public ResultData<Boolean> leaveSittingRoom(@RequestBody Map<String, Object> body) {
        try {
            doctorStationService.leaveSittingRoom(
                    str(body, "doctorId", "id"),
                    str(body, "officeRoomId", "roomId"));
            return ResultData.success(true, "已离岗");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    /**
     * 专家临时停诊；body：{ doctorId, remark? }
     * 消费者：主任台门诊、护士站
     */
    @PostMapping("/SuspendDuty")
    public ResultData<Boolean> suspendDuty(@RequestBody Map<String, Object> body) {
        try {
            doctorStationService.suspendDuty(str(body, "doctorId", "id"), str(body, "remark", "reason"));
            return ResultData.success(true, "已临时停诊");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    /**
     * 主任台取消临时停诊；body：{ doctorId }
     * 清除当日 OFF 排班，医生需重新登录叫号器开诊。
     */
    @PostMapping("/ResumeDuty")
    public ResultData<Boolean> resumeDuty(@RequestBody Map<String, Object> body) {
        try {
            doctorStationService.resumeDuty(str(body, "doctorId", "id"));
            return ResultData.success(true, "已取消停诊");
        } catch (IllegalArgumentException e) {
            return ResultData.fail(400, e.getMessage());
        }
    }

    private static String str(Map<String, Object> body, String key, String alt) {
        if (body == null) {
            return "";
        }
        Object v = body.get(key);
        if (v == null && alt != null) {
            v = body.get(alt);
        }
        return v == null ? "" : v.toString().trim();
    }

    private static String extractWorkNo(Map<String, Object> body) {
        if (body == null) {
            return "";
        }
        Object v = body.get("workNo");
        if (v == null) {
            v = body.get("ASID");
        }
        return v == null ? "" : v.toString().trim();
    }

    private Optional<Doctor> lookupDoctor(String key) {
        List<Doctor> list = doctorRepository.findByWorkNo(key);
        if (!list.isEmpty()) {
            return Optional.of(list.get(0));
        }
        return doctorRepository.findById(key);
    }

    // ── 诊室 ──────────────────────────────────────────────────

    @PostMapping("/GetAllOffice")
    public ResultData<List<OfficeRoom>> getAllOffice() {
        return ResultData.success(officeRoomRepository.findAll(), "获取成功");
    }

    @PostMapping("/GetOfficeByDept")
    public ResultData<List<OfficeRoom>> getOfficeByDept(@RequestParam("deptId") String deptId) {
        return ResultData.success(officeRoomRepository.findByDeptId(deptId), "获取成功");
    }

    @PostMapping("/SaveOffice")
    public ResultData<OfficeRoom> saveOffice(@RequestBody OfficeRoom room) {
        return ResultData.success(officeRoomRepository.save(room), "保存成功");
    }

    @PostMapping("/syncFromHis")
    public ResultData<Map<String, Object>> syncFromHis(@RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> result = hisSyncService.saveAndSync(HisSyncService.CONFIG_KEY_DOCTOR, body, false);
            return ResultData.success(result, String.valueOf(result.get("message")));
        } catch (Exception e) {
            return ResultData.fail(500, e.getMessage());
        }
    }

    @PostMapping("/probeHisSync")
    public ResultData<Map<String, Object>> probeHisSync(@RequestBody Map<String, Object> body) {
        return ResultData.success(hisSyncService.probe(HisSyncService.CONFIG_KEY_DOCTOR, body, false), "拨测完成");
    }

    @GetMapping("/getHisSyncConfig")
    public ResultData<Map<String, Object>> getHisSyncConfig() {
        return ResultData.success(hisSyncService.getSyncConfig(HisSyncService.CONFIG_KEY_DOCTOR), "获取成功");
    }
}
