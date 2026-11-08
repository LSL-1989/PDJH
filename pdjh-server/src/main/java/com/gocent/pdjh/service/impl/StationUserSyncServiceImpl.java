package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.entity.Department;
import com.gocent.pdjh.entity.Doctor;
import com.gocent.pdjh.entity.OfficeRoom;
import com.gocent.pdjh.entity.Users;
import com.gocent.pdjh.enums.LoginScene;
import com.gocent.pdjh.repository.DepartmentRepository;
import com.gocent.pdjh.repository.DoctorRepository;
import com.gocent.pdjh.repository.UserRepository;
import com.gocent.pdjh.service.StationUserSyncService;
import com.gocent.pdjh.util.PasswordHashes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 医生档案 → users 开户/关联。已有同工号帐号则复用（含后台 ADMIN），不另建双密码。
 */
@Service
@RequiredArgsConstructor
public class StationUserSyncServiceImpl implements StationUserSyncService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional
    public Users ensureUser(Doctor doctor) {
        if (doctor == null) {
            throw new IllegalArgumentException("医生档案不能为空");
        }
        String workNo = trimToNull(doctor.getWorkNo());
        if (workNo == null) {
            throw new IllegalArgumentException("工号不能为空");
        }
        String userNo = trimToNull(doctor.getUserNo());
        if (userNo == null) {
            userNo = workNo;
        }
        Users user = userRepository.findById(userNo).orElse(null);
        if (user == null) {
            String phone = trimToNull(doctor.getPhone());
            if (phone != null) {
                user = userRepository.findByPhone(phone).orElse(null);
            }
        }
        boolean created = false;
        if (user == null) {
            user = new Users();
            user.setUserNo(userNo);
            user.setCreateDate(new Date());
            created = true;
        }
        user.setUserName(doctor.getName());
        if (trimToNull(doctor.getPhone()) != null) {
            user.setPhone(doctor.getPhone().trim());
        }
        if (trimToNull(doctor.getDeptId()) != null) {
            user.setDeptID(doctor.getDeptId().trim());
        }
        int enabled = doctor.getDisplayStatus() != null && doctor.getDisplayStatus() == 0 ? 0 : 1;
        if (user.getState() == null || created) {
            user.setState(enabled);
        }
        String scene = loginSceneFromDeptId(doctor.getDeptId());
        if (created || !LoginScene.ADMIN.getCode().equalsIgnoreCase(user.getLoginScene())) {
            if (user.getLoginScene() == null || user.getLoginScene().isBlank() || created) {
                user.setLoginScene(scene);
            }
        }
        if (user.getPassWord() == null || user.getPassWord().isBlank()) {
            String raw = trimToNull(doctor.getPassword());
            if (raw == null) {
                raw = Users.DEFAULT_PASSWORD;
            }
            user.setPassWord(PasswordHashes.hash(raw));
            if (user.getMustChangePassword() == null) {
                user.setMustChangePassword(1);
            }
        } else if (!PasswordHashes.isHashed(user.getPassWord())) {
            user.setPassWord(PasswordHashes.hash(user.getPassWord()));
        }
        Users saved = userRepository.save(user);

        boolean doctorDirty = false;
        if (!saved.getUserNo().equals(doctor.getUserNo())) {
            doctor.setUserNo(saved.getUserNo());
            doctorDirty = true;
        }
        if (doctor.getPassword() != null && !doctor.getPassword().isBlank()) {
            doctor.setPassword(null);
            doctorDirty = true;
        }
        if (doctor.getId() != null && doctorDirty) {
            doctorRepository.save(doctor);
        }
        return saved;
    }

    @Override
    public String stationModeFromBusinessType(String businessType) {
        if (businessType == null || businessType.isBlank()) {
            return "CLINIC";
        }
        String t = businessType.trim().toUpperCase();
        if ("PHARMACY".equals(t)) {
            return "PHARMACY";
        }
        if ("LAB".equals(t)) {
            return "LAB";
        }
        if (isExamType(t)) {
            return "EXAM";
        }
        return "CLINIC";
    }

    @Override
    public String stationModeFromRoom(OfficeRoom room) {
        if (room == null) {
            return "CLINIC";
        }
        if (room.getBusinessType() != null && !room.getBusinessType().isBlank()) {
            return stationModeFromBusinessType(room.getBusinessType());
        }
        if (room.getDeptId() != null && !room.getDeptId().isBlank()) {
            return stationModeFromBusinessType(
                    departmentRepository.findById(room.getDeptId())
                            .map(Department::getBusinessType)
                            .orElse(null));
        }
        return "CLINIC";
    }

    @Override
    public String loginSceneFromBusinessType(String businessType) {
        String mode = stationModeFromBusinessType(businessType);
        if ("CLINIC".equals(mode)) {
            return LoginScene.DOCTOR.getCode();
        }
        return mode;
    }

    @Override
    public Users findUserByAccount(String account) {
        String key = trimToNull(account);
        if (key == null) {
            return null;
        }
        Users user = userRepository.findById(key).orElse(null);
        if (user != null) {
            return user;
        }
        return userRepository.findByPhone(key).orElse(null);
    }

    @Override
    public Optional<Doctor> findLinkedDoctor(Users user) {
        if (user == null) {
            return Optional.empty();
        }
        String userNo = trimToNull(user.getUserNo());
        if (userNo != null) {
            List<Doctor> byUserNo = doctorRepository.findByUserNo(userNo);
            if (!byUserNo.isEmpty()) {
                return Optional.of(byUserNo.get(0));
            }
            List<Doctor> byWork = doctorRepository.findByWorkNo(userNo);
            if (!byWork.isEmpty()) {
                return Optional.of(byWork.get(0));
            }
        }
        String phone = trimToNull(user.getPhone());
        if (phone != null) {
            List<Doctor> byPhone = doctorRepository.findByPhone(phone);
            if (!byPhone.isEmpty()) {
                return Optional.of(byPhone.get(0));
            }
        }
        return Optional.empty();
    }

    @Override
    @Transactional
    public Doctor ensureDoctor(Users user) {
        if (user == null) {
            throw new IllegalArgumentException("用户不能为空");
        }
        Optional<Doctor> existing = findLinkedDoctor(user);
        if (existing.isPresent()) {
            Doctor doctor = existing.get();
            boolean dirty = false;
            if (trimToNull(doctor.getUserNo()) == null) {
                doctor.setUserNo(user.getUserNo());
                dirty = true;
            }
            if (dirty && doctor.getId() != null) {
                doctorRepository.save(doctor);
            }
            return doctor;
        }
        Doctor doctor = new Doctor();
        doctor.setId(UUID.randomUUID().toString().replace("-", ""));
        doctor.setWorkNo(user.getUserNo());
        doctor.setUserNo(user.getUserNo());
        doctor.setName(user.getUserName());
        doctor.setPhone(user.getPhone());
        doctor.setDeptId(user.getDeptID());
        doctor.setDisplayStatus(user.getState() != null && user.getState() == 0 ? 0 : 1);
        doctor.setIsOnDuty(0);
        doctor.setMustChangePassword(0);
        doctor.setPassword(null);
        doctor.setCreateDate(new Date());
        return doctorRepository.save(doctor);
    }

    private String loginSceneFromDeptId(String deptId) {
        if (deptId == null || deptId.isBlank()) {
            return LoginScene.DOCTOR.getCode();
        }
        return departmentRepository.findById(deptId.trim())
                .map(d -> loginSceneFromBusinessType(d.getBusinessType()))
                .orElse(LoginScene.DOCTOR.getCode());
    }

    private static boolean isExamType(String t) {
        return "EXAM".equals(t) || "MEDTECH".equals(t)
                || "RADIOLOGY".equals(t) || "ULTRASOUND".equals(t) || "ECG".equals(t);
    }

    private static String trimToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
