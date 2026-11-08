package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.dto.DoctorLoginVO;
import com.gocent.pdjh.dto.SittingDoctorVO;
import com.gocent.pdjh.entity.Department;
import com.gocent.pdjh.entity.Doctor;
import com.gocent.pdjh.entity.DoctorDept;
import com.gocent.pdjh.entity.DoctorSchedule;
import com.gocent.pdjh.entity.OfficeRoom;
import com.gocent.pdjh.entity.Users;
import com.gocent.pdjh.enums.LoginScene;
import com.gocent.pdjh.exception.LoginDeniedException;
import com.gocent.pdjh.repository.DepartmentRepository;
import com.gocent.pdjh.repository.DoctorDeptRepository;
import com.gocent.pdjh.repository.DoctorRepository;
import com.gocent.pdjh.repository.DoctorScheduleRepository;
import com.gocent.pdjh.repository.OfficeRoomRepository;
import com.gocent.pdjh.repository.UserRepository;
import com.gocent.pdjh.service.DoctorStationService;
import com.gocent.pdjh.service.LoginLogService;
import com.gocent.pdjh.service.ShiftTypeService;
import com.gocent.pdjh.service.StationUserSyncService;
import com.gocent.pdjh.util.PasswordHashes;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 叫号器认证与坐诊绑定。登录优先校验 users（BCrypt + login_scene），doctor 只提供档案与诊室。
 * 消费者：HJ叫号器 WPF；坐诊变更经 /topic/hall 通知门边屏
 */
@Service
@RequiredArgsConstructor
public class DoctorStationServiceImpl implements DoctorStationService {

    private final DoctorRepository doctorRepository;
    private final OfficeRoomRepository officeRoomRepository;
    private final DoctorDeptRepository doctorDeptRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final ShiftTypeService shiftTypeService;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final StationUserSyncService stationUserSyncService;
    private final LoginLogService loginLogService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 叫号器登录：优先校验 users（BCrypt）；无档案的窗口岗位补最小 doctor；兼容期从 doctor 迁入。
     * 入参 account=工号或手机号；出参含档案、诊室列表、stationMode（优先默认诊室 businessType）。
     * ADMIN/NURSE 账号拒绝，须走对应 Web 客户端（§23.8）。
     */
    @Override
    @Transactional
    public DoctorLoginVO login(String account, String password) {
        if (account == null || account.isBlank() || password == null) {
            loginLogService.record(account, "DOCTOR", false, "用户名或密码不能为空");
            throw new LoginDeniedException("用户名或密码不能为空");
        }
        String key = account.trim();
        Users user = stationUserSyncService.findUserByAccount(key);
        Doctor doctor = user != null
                ? stationUserSyncService.findLinkedDoctor(user).orElse(null)
                : findByAccount(key).orElse(null);
        if (user == null && doctor != null) {
            user = stationUserSyncService.ensureUser(doctor);
        }
        if (user == null) {
            loginLogService.record(key, "DOCTOR", false, "用户名或密码错误");
            throw new LoginDeniedException("用户名或密码错误");
        }
        if (!PasswordHashes.matches(password, user.getPassWord())) {
            loginLogService.record(user.getUserNo(), sceneOf(user), false, "用户名或密码错误");
            throw new LoginDeniedException("用户名或密码错误");
        }
        if (!PasswordHashes.isHashed(user.getPassWord())) {
            user.setPassWord(PasswordHashes.hash(password));
            userRepository.save(user);
        }
        LoginScene scene = LoginScene.fromCode(user.getLoginScene());
        if (scene != null && !scene.isCallStation()) {
            loginLogService.record(user.getUserNo(), sceneOf(user), false, "请使用对应客户端登录");
            throw new LoginDeniedException("该账号请使用对应客户端登录（管理后台或护士站）");
        }
        if (user.getState() != null && user.getState() != 1) {
            loginLogService.record(user.getUserNo(), sceneOf(user), false, "账号已禁用");
            throw new LoginDeniedException("账号已禁用，请联系管理员");
        }
        if (doctor == null) {
            doctor = stationUserSyncService.ensureDoctor(user);
        } else {
            stationUserSyncService.ensureUser(doctor);
        }
        if (doctor.getDisplayStatus() != null && doctor.getDisplayStatus() == 0) {
            loginLogService.record(user.getUserNo(), sceneOf(user), false, "账号已冻结");
            throw new LoginDeniedException("账号已冻结，请联系管理员");
        }
        loginLogService.record(user.getUserNo(), sceneOf(user), true, null);
        DoctorLoginVO vo = toLoginVO(doctor);
        vo.setMustChangePassword(user.getMustChangePassword() != null ? user.getMustChangePassword() : 1);
        return vo;
    }

    @Override
    @Transactional
    public void changePassword(String doctorId, String oldPassword, String newPassword) {
        if (doctorId == null || doctorId.isBlank()) {
            throw new IllegalArgumentException("医生 id 不能为空");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("新密码不能为空");
        }
        if (newPassword.trim().length() < 4) {
            throw new IllegalArgumentException("新密码至少 4 位");
        }
        Doctor doctor = doctorRepository.findById(doctorId.trim())
                .orElseThrow(() -> new IllegalArgumentException("医生不存在"));
        Users user = stationUserSyncService.ensureUser(doctor);
        boolean force = user.getMustChangePassword() != null && user.getMustChangePassword() == 1;
        if (!force) {
            if (oldPassword == null || !PasswordHashes.matches(oldPassword, user.getPassWord())) {
                throw new IllegalArgumentException("原密码错误");
            }
        } else if (oldPassword != null && !oldPassword.isBlank()
                && !PasswordHashes.matches(oldPassword, user.getPassWord())) {
            throw new IllegalArgumentException("原密码错误");
        }
        user.setPassWord(PasswordHashes.hash(newPassword.trim()));
        user.setMustChangePassword(0);
        user.setPasswordResetRequested(0);
        user.setPasswordResetRequestTime(null);
        userRepository.save(user);
        doctor.setPassword(null);
        doctor.setMustChangePassword(0);
        doctor.setPasswordResetRequested(0);
        doctor.setPasswordResetRequestTime(null);
        doctorRepository.save(doctor);
    }

    @Override
    @Transactional
    public void requestPasswordReset(String account) {
        if (account == null || account.isBlank()) {
            throw new IllegalArgumentException("请输入工号或手机号");
        }
        String key = account.trim();
        boolean found = false;
        // 优先写统一用户表，供后台「用户管理」处理（药房/已建账户的医护）
        Users user = userRepository.findById(key).orElse(null);
        if (user == null) {
            user = userRepository.findByPhone(key).orElse(null);
        }
        if (user != null) {
            user.setPasswordResetRequested(1);
            user.setPasswordResetRequestTime(new Date());
            userRepository.save(user);
            found = true;
        }
        // 医生档案仍保留申请标记（尚未迁入 users 的历史账号）
        Optional<Doctor> doctorOpt = findByAccount(key);
        if (doctorOpt.isPresent()) {
            Doctor doctor = doctorOpt.get();
            doctor.setPasswordResetRequested(1);
            doctor.setPasswordResetRequestTime(new Date());
            doctorRepository.save(doctor);
            found = true;
        }
        if (!found) {
            throw new IllegalArgumentException("未找到该工号/手机号对应的账号");
        }
    }

    @Override
    @Transactional
    public void adminResetPassword(String doctorId) {
        if (doctorId == null || doctorId.isBlank()) {
            throw new IllegalArgumentException("医生 id 不能为空");
        }
        Doctor doctor = doctorRepository.findById(doctorId.trim())
                .orElseThrow(() -> new IllegalArgumentException("医生不存在"));
        Users user = stationUserSyncService.ensureUser(doctor);
        user.setPassWord(PasswordHashes.hash(Users.DEFAULT_PASSWORD));
        user.setMustChangePassword(1);
        user.setPasswordResetRequested(0);
        user.setPasswordResetRequestTime(null);
        userRepository.save(user);
        doctor.setPassword(null);
        doctor.setMustChangePassword(1);
        doctor.setPasswordResetRequested(0);
        doctor.setPasswordResetRequestTime(null);
        doctorRepository.save(doctor);
    }

    @Override
    @Transactional
    public OfficeRoom bindSittingRoom(String doctorId, String officeRoomId) {
        if (doctorId == null || doctorId.isBlank() || officeRoomId == null || officeRoomId.isBlank()) {
            throw new IllegalArgumentException("医生与诊室不能为空");
        }
        Doctor doctor = doctorRepository.findById(doctorId.trim())
                .orElseThrow(() -> new IllegalArgumentException("医生不存在"));
        OfficeRoom room = officeRoomRepository.findById(officeRoomId.trim())
                .orElseThrow(() -> new IllegalArgumentException("诊室不存在"));
        if (room.getState() != null && room.getState() == 0) {
            throw new IllegalArgumentException("诊室已关闭，请选择其他诊室");
        }
        // 解除该医生在其他诊室的绑定，避免一门多挂
        for (OfficeRoom other : officeRoomRepository.findByDoctorId(doctor.getId())) {
            if (!other.getId().equals(room.getId())) {
                other.setDoctorId(null);
                officeRoomRepository.save(other);
            }
        }
        // 原坐诊医生被顶替时标记离诊
        if (room.getDoctorId() != null && !room.getDoctorId().isBlank()
                && !room.getDoctorId().equals(doctor.getId())) {
            doctorRepository.findById(room.getDoctorId()).ifPresent(prev -> {
                if (prev.getIsOnDuty() != null && prev.getIsOnDuty() == 1) {
                    prev.setIsOnDuty(0);
                    doctorRepository.save(prev);
                }
            });
        }
        room.setDoctorId(doctor.getId());
        officeRoomRepository.save(room);
        doctor.setOfficeName(room.getName());
        doctor.setIsOnDuty(1);
        doctorRepository.save(doctor);
        broadcastSittingChange();
        return room;
    }

    @Override
    @Transactional
    public void leaveSittingRoom(String doctorId, String officeRoomId) {
        if (doctorId == null || doctorId.isBlank()) {
            throw new IllegalArgumentException("医生 id 不能为空");
        }
        Doctor doctor = doctorRepository.findById(doctorId.trim())
                .orElseThrow(() -> new IllegalArgumentException("医生不存在"));
        for (OfficeRoom room : officeRoomRepository.findByDoctorId(doctor.getId())) {
            room.setDoctorId(null);
            officeRoomRepository.save(room);
        }
        if (officeRoomId != null && !officeRoomId.isBlank()) {
            officeRoomRepository.findById(officeRoomId.trim()).ifPresent(room -> {
                if (room.getDoctorId() == null || room.getDoctorId().isBlank()
                        || doctor.getId().equals(room.getDoctorId())) {
                    room.setDoctorId(null);
                    officeRoomRepository.save(room);
                }
            });
        }
        doctor.setIsOnDuty(0);
        doctor.setOfficeName(null);
        doctorRepository.save(doctor);
        broadcastSittingChange();
    }

    /**
     * 专家临时停诊：离诊 + isOnDuty=0 + 当日排班标记 OFF（若无则新建）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspendDuty(String doctorId, String remark) {
        if (doctorId == null || doctorId.isBlank()) {
            throw new IllegalArgumentException("doctorId 不能为空");
        }
        leaveSittingRoom(doctorId, null);
        LocalDate today = LocalDate.now();
        doctorScheduleRepository.deleteByDoctorIdAndScheduleDate(doctorId.trim(), today);
        DoctorSchedule off = new DoctorSchedule();
        off.setDoctorId(doctorId.trim());
        off.setScheduleDate(today);
        off.setShiftType("OFF");
        off.setRemark(remark != null ? remark : "临时停诊");
        doctorRepository.findById(doctorId.trim()).ifPresent(d -> off.setDeptId(d.getDeptId()));
        doctorScheduleRepository.save(off);
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("type", "DOCTOR_SUSPEND");
        msg.put("doctorId", doctorId.trim());
        msg.put("message", "医生临时停诊");
        messagingTemplate.convertAndSend("/topic/hall", (Object) msg);
    }

    /**
     * 主任台取消临时停诊：清除当日 OFF 排班；不自动 isOnDuty=1（须重新 BindSittingRoom）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resumeDuty(String doctorId) {
        if (doctorId == null || doctorId.isBlank()) {
            throw new IllegalArgumentException("doctorId 不能为空");
        }
        String id = doctorId.trim();
        LocalDate today = LocalDate.now();
        List<DoctorSchedule> todaySched = doctorScheduleRepository.findByDoctorIdAndScheduleDate(id, today);
        boolean hadOff = todaySched.stream().anyMatch(s -> "OFF".equalsIgnoreCase(s.getShiftType()));
        if (!hadOff) {
            throw new IllegalArgumentException("该医生今日无临时停诊记录");
        }
        doctorScheduleRepository.deleteByDoctorIdAndScheduleDate(id, today);
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("type", "DOCTOR_RESUME");
        msg.put("doctorId", id);
        msg.put("message", "医生取消停诊");
        messagingTemplate.convertAndSend("/topic/hall", (Object) msg);
    }

    /** 通知门边屏立即拉取；无 code 时各屏均刷新（与护士台 STATE_UPDATE 一致） */
    private void broadcastSittingChange() {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("type", "STATE_UPDATE");
        messagingTemplate.convertAndSend("/topic/hall", (Object) msg);
    }

    @Override
    public List<SittingDoctorVO> listSittingDoctorsByDept(String deptId) {
        if (deptId == null || deptId.isBlank()) {
            return List.of();
        }
        String dept = deptId.trim();
        String deptName = departmentRepository.findById(dept).map(Department::getName).orElse(dept);
        List<OfficeRoom> rooms = officeRoomRepository.findByDeptId(dept);
        List<SittingDoctorVO> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (OfficeRoom room : rooms) {
            if (room.getState() != null && room.getState() == 0) continue;
            String did = room.getDoctorId();
            if (did == null || did.isBlank() || !seen.add(did)) continue;
            Doctor doctor = doctorRepository.findById(did).orElse(null);
            if (doctor == null) continue;
            if (doctor.getDisplayStatus() != null && doctor.getDisplayStatus() == 0) continue;
            if (doctor.getIsOnDuty() == null || doctor.getIsOnDuty() != 1) continue;
            SittingDoctorVO vo = new SittingDoctorVO();
            vo.setDoctorId(doctor.getId());
            vo.setWorkNo(doctor.getWorkNo());
            vo.setName(doctor.getName());
            vo.setTitle(doctor.getTitle());
            vo.setDeptId(dept);
            vo.setDeptName(deptName);
            vo.setOfficeRoomId(room.getId());
            vo.setOfficeRoomName(room.getName());
            vo.setAvatar(doctor.getAvatar());
            out.add(vo);
        }
        return out;
    }

    private Optional<Doctor> findByAccount(String account) {
        List<Doctor> byWork = doctorRepository.findByWorkNo(account);
        if (!byWork.isEmpty()) {
            return Optional.of(byWork.get(0));
        }
        List<Doctor> byPhone = doctorRepository.findByPhone(account);
        if (!byPhone.isEmpty()) {
            return Optional.of(byPhone.get(0));
        }
        return Optional.empty();
    }

    private DoctorLoginVO toLoginVO(Doctor doctor) {
        DoctorLoginVO vo = new DoctorLoginVO();
        vo.setDoctorId(doctor.getId());
        vo.setWorkNo(doctor.getWorkNo());
        vo.setName(doctor.getName());
        vo.setPhone(doctor.getPhone());
        vo.setDeptId(doctor.getDeptId());
        vo.setTitle(doctor.getTitle());
        vo.setMustChangePassword(doctor.getMustChangePassword() != null ? doctor.getMustChangePassword() : 1);

        Set<String> deptIds = new LinkedHashSet<>();
        doctorDeptRepository.findByDoctorId(doctor.getId()).forEach(dd -> deptIds.add(dd.getDeptId()));
        if (doctor.getDeptId() != null && !doctor.getDeptId().isBlank()) {
            deptIds.add(doctor.getDeptId());
        }

        String resolvedBusinessType = null;
        boolean lab = false;
        for (String deptId : deptIds) {
            Optional<Department> deptOpt = departmentRepository.findById(deptId);
            if (deptOpt.isEmpty()) continue;
            String bt = deptOpt.get().getBusinessType();
            if (bt == null) continue;
            if (resolvedBusinessType == null) resolvedBusinessType = bt;
            if ("PHARMACY".equalsIgnoreCase(bt)) {
                resolvedBusinessType = "PHARMACY";
                break;
            }
            if ("LAB".equalsIgnoreCase(bt)) {
                lab = true;
                resolvedBusinessType = "LAB";
            }
            if (isExamStationType(bt) && !lab) {
                resolvedBusinessType = bt;
            }
        }
        vo.setBusinessType(resolvedBusinessType != null ? resolvedBusinessType : "OUTPATIENT");

        List<OfficeRoom> rooms = deptIds.stream()
                .flatMap(deptId -> officeRoomRepository.findByDeptId(deptId).stream())
                .filter(r -> r.getState() == null || r.getState() != 0)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(OfficeRoom::getId, r -> r, (a, b) -> a, LinkedHashMap::new),
                        m -> new java.util.ArrayList<>(m.values())));
        vo.setRooms(rooms);

        // 当日可能上午、下午各一间诊室，按当前时刻选对应班次
        OfficeRoom scheduleDefault = null;
        List<DoctorSchedule> todaySched = doctorScheduleRepository.findByDoctorIdAndScheduleDate(doctor.getId(), LocalDate.now(ZoneId.of("Asia/Shanghai")));
        String roomId = shiftTypeService.pickOfficeRoomId(todaySched, LocalTime.now(ZoneId.of("Asia/Shanghai"))).orElse(null);
        if (roomId != null) {
            scheduleDefault = officeRoomRepository.findById(roomId)
                    .filter(room -> room.getState() == null || room.getState() != 0)
                    .orElse(null);
        }
        if (scheduleDefault != null) {
            vo.setDefaultRoom(scheduleDefault);
        } else {
            List<OfficeRoom> bound = officeRoomRepository.findByDoctorId(doctor.getId()).stream()
                    .filter(r -> r.getState() == null || r.getState() != 0)
                    .collect(Collectors.toList());
            vo.setDefaultRoom(bound.isEmpty() ? (rooms.isEmpty() ? null : rooms.get(0)) : bound.get(0));
        }
        // stationMode 以上岗窗口为准；登录时尚无绑定时用默认诊室，再回退科室
        if (vo.getDefaultRoom() != null) {
            vo.setStationMode(stationUserSyncService.stationModeFromRoom(vo.getDefaultRoom()));
        } else {
            vo.setStationMode(stationUserSyncService.stationModeFromBusinessType(resolvedBusinessType));
        }
        return vo;
    }

    private static String sceneOf(Users user) {
        return user.getLoginScene() != null && !user.getLoginScene().isBlank()
                ? user.getLoginScene() : "DOCTOR";
    }

    /** 医技/体检检查室：与门诊诊室共用列表版叫号器，stationMode=EXAM */
    private static boolean isExamStationType(String businessType) {
        if (businessType == null || businessType.isBlank()) {
            return false;
        }
        String t = businessType.trim().toUpperCase();
        return "EXAM".equals(t) || "MEDTECH".equals(t)
                || "RADIOLOGY".equals(t) || "ULTRASOUND".equals(t) || "ECG".equals(t);
    }
}
