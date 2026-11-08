package com.gocent.pdjh.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gocent.pdjh.dto.clinic.ClinicDisplayVO;
import com.gocent.pdjh.dto.clinic.ClinicDoctorVO;
import com.gocent.pdjh.dto.clinic.ClinicPatientRowVO;
import com.gocent.pdjh.dto.clinic.ClinicRoomVO;
import com.gocent.pdjh.dto.clinic.ClinicScreenVO;
import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.entity.Department;
import com.gocent.pdjh.entity.DocScreen;
import com.gocent.pdjh.entity.Doctor;
import com.gocent.pdjh.entity.OfficeRoom;
import com.gocent.pdjh.exception.TerminalBindingException;
import com.gocent.pdjh.repository.CallListRepository;
import com.gocent.pdjh.repository.DepartmentRepository;
import com.gocent.pdjh.repository.DocScreenRepository;
import com.gocent.pdjh.repository.DoctorRepository;
import com.gocent.pdjh.repository.OfficeRoomRepository;
import com.gocent.pdjh.repository.ScreenRoomMappingRepository;
import com.gocent.pdjh.service.ClinicScreenService;
import com.gocent.pdjh.service.ScreenLifecycleService;
import com.gocent.pdjh.service.SysConfigService;
import com.gocent.pdjh.service.support.ClinicDisplayConfigSupport;
import com.gocent.pdjh.utils.DesensitizeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClinicScreenServiceImpl implements ClinicScreenService {

    private static final int WAITING_LIMIT = 8;

    private final DocScreenRepository docScreenRepository;
    private final ScreenRoomMappingRepository screenRoomMappingRepository;
    private final OfficeRoomRepository officeRoomRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final CallListRepository callListRepository;
    private final SysConfigService sysConfigService;
    private final ScreenLifecycleService screenLifecycleService;
    private final ObjectMapper objectMapper;

    @Override
    public ClinicDisplayVO buildDisplay(String deviceCode) {
        return buildDisplay(deviceCode, false);
    }

    @Override
    public ClinicDisplayVO buildDisplay(String deviceCode, boolean preview) {
        DocScreen screen = docScreenRepository.findByDeviceCode(deviceCode).orElse(null);
        if (screen == null) {
            throw new TerminalBindingException(404, "设备未注册，请联系管理员");
        }
        // 现场轮询才刷新 lastSeenAt；预览只读，不把管理端窗口算成设备在线
        if (!preview) {
            screenLifecycleService.touchLastSeen(screen.getId());
        }
        if (screen.getState() != null && screen.getState() == 0) {
            throw new TerminalBindingException(403, "设备已停用");
        }
        if (screen.getState() != null && screen.getState() == 2) {
            throw new TerminalBindingException(428, "设备待配置，请联系管理员");
        }

        List<String> roomIds = screenRoomMappingRepository.findByScreenId(screen.getId()).stream()
                .map(m -> m.getOfficeRoomId())
                .collect(Collectors.toList());
        if (roomIds.isEmpty()) {
            throw new TerminalBindingException(403, "尚未绑定物理诊室");
        }

        String roomId = roomIds.get(0);
        OfficeRoom room = officeRoomRepository.findById(roomId)
                .orElseThrow(() -> new TerminalBindingException(403, "绑定的物理诊室不存在"));

        Map<String, Object> displayCfg = ClinicDisplayConfigSupport.load(sysConfigService, objectMapper, screen.getId());
        boolean desensitize = ClinicDisplayConfigSupport.nameDesensitize(displayCfg);
        boolean showWaitList = ClinicDisplayConfigSupport.showWaitList(displayCfg);

        ClinicDisplayVO vo = new ClinicDisplayVO();
        vo.setHospitalName(resolveHospitalName());
        vo.setHospitalLogo(resolveHospitalLogo());
        vo.setScreen(buildScreenVO(screen, displayCfg));
        vo.setRoom(buildRoomVO(room));
        vo.setDoctors(resolveDoctors(room));
        vo.setCurrentPatients(buildCurrentPatients(roomId, desensitize));
        vo.setWaitingPatients(showWaitList ? buildWaitingPatients(roomId, desensitize) : List.of());
        return vo;
    }

    private ClinicScreenVO buildScreenVO(DocScreen screen, Map<String, Object> displayCfg) {
        ClinicScreenVO vo = new ClinicScreenVO();
        vo.setId(screen.getId());
        vo.setState(screen.getState());
        vo.setBulletin(screen.getBulletin() != null && !screen.getBulletin().isBlank()
                ? screen.getBulletin()
                : "");
        vo.setScrollIntervalSec(ClinicDisplayConfigSupport.scrollIntervalSec(displayCfg, screen.getScrollTime()));
        vo.setCallCode(screen.getCallCode());
        vo.setNameDesensitize(ClinicDisplayConfigSupport.nameDesensitize(displayCfg));
        vo.setShowWaitList(ClinicDisplayConfigSupport.showWaitList(displayCfg));
        return vo;
    }

    private ClinicRoomVO buildRoomVO(OfficeRoom room) {
        ClinicRoomVO vo = new ClinicRoomVO();
        vo.setId(room.getId());
        vo.setName(room.getName());
        vo.setDeptId(room.getDeptId());
        vo.setFloor(room.getFloor() != null ? room.getFloor() : "");
        vo.setRoomNo(room.getRoomNo() != null ? room.getRoomNo() : "");
        if (room.getDeptId() != null) {
            departmentRepository.findById(room.getDeptId())
                    .map(Department::getName)
                    .ifPresent(vo::setDeptName);
        }
        if (vo.getDeptName() == null) {
            vo.setDeptName("");
        }
        return vo;
    }

    /**
     * 坐诊医生仅以 office_room.doctor_id 为准（工作站 Bind/LeaveSittingRoom 写入）。
     * 无绑定则空列表 → 门边屏「暂无值班」；不再用 call_list 回落，避免离岗后仍显示值班人。
     * 双人诊室：绑定医生为主，再补当日 call_list 中其他在岗医生（最多 2 人）。
     */
    private List<ClinicDoctorVO> resolveDoctors(OfficeRoom room) {
        List<ClinicDoctorVO> doctors = new ArrayList<>();
        if (room.getDoctorId() == null || room.getDoctorId().isBlank()) {
            return doctors;
        }
        LinkedHashSet<String> doctorIds = new LinkedHashSet<>();
        doctorIds.add(room.getDoctorId());
        collectDoctorIdsFromCallList(room.getId(), doctorIds);
        for (String doctorId : doctorIds) {
            if (doctors.size() >= 2) break;
            doctorRepository.findById(doctorId).ifPresent(d -> {
                if (doctors.size() >= 2) return;
                // 坐诊展示须在岗：离诊后 isOnDuty=0，即使 doctor_id 未清也不再显示医生信息
                boolean onDuty = d.getIsOnDuty() != null && d.getIsOnDuty() == 1;
                if (onDuty) {
                    doctors.add(toDoctorVO(d));
                }
            });
        }
        return doctors;
    }

    private void collectDoctorIdsFromCallList(String roomId, Set<String> doctorIds) {
        List<CallList> all = new ArrayList<>();
        all.addAll(callListRepository.findTodayCallListByOfficeRoomAndState(roomId, 0));
        all.addAll(callListRepository.findTodayCallListByOfficeRoomAndState(roomId, 1));
        for (CallList c : all) {
            if (c.getDoctorID() != null && !c.getDoctorID().isBlank()) {
                doctorIds.add(c.getDoctorID());
            }
        }
    }

    private String resolveHospitalName() {
        return configValue("HospitalName");
    }

    private String resolveHospitalLogo() {
        return configValue("HospitalLogo");
    }

    private String configValue(String key) {
        var cfg = sysConfigService.getConfigByKey(key);
        if (cfg == null || cfg.getConfigValue() == null) {
            return "";
        }
        return cfg.getConfigValue().trim();
    }

    private ClinicDoctorVO toDoctorVO(Doctor d) {
        ClinicDoctorVO vo = new ClinicDoctorVO();
        vo.setId(d.getId());
        vo.setName(d.getName());
        vo.setTitle(d.getTitle() != null ? d.getTitle() : "");
        vo.setAvatar(d.getAvatar() != null ? d.getAvatar() : "");
        vo.setRemarks(d.getRemarks() != null ? d.getRemarks() : "");
        vo.setSpecialty(d.getSpecialty() != null ? d.getSpecialty() : "");
        vo.setIsOnDuty(d.getIsOnDuty() != null ? d.getIsOnDuty() : 1);
        return vo;
    }

    private List<ClinicPatientRowVO> buildCurrentPatients(String roomId, boolean desensitize) {
        return callListRepository.findTodayCallListByOfficeRoomAndState(roomId, 1).stream()
                .sorted(Comparator.comparing(c -> c.getXh() != null ? c.getXh() : 0))
                .map(c -> toPatientRow(c, desensitize))
                .collect(Collectors.toList());
    }

    private List<ClinicPatientRowVO> buildWaitingPatients(String roomId, boolean desensitize) {
        return callListRepository.findTodayCallListByOfficeRoomAndState(roomId, 0).stream()
                .filter(c -> c.getTriageState() == null
                        || "WAITING".equals(c.getTriageState())
                        || "REVISIT".equals(c.getTriageState()))
                .sorted(Comparator.comparing(c -> c.getXh() != null ? c.getXh() : 0))
                .limit(WAITING_LIMIT)
                .map(c -> toPatientRow(c, desensitize))
                .collect(Collectors.toList());
    }

    private ClinicPatientRowVO toPatientRow(CallList c, boolean desensitize) {
        ClinicPatientRowVO vo = new ClinicPatientRowVO();
        vo.setDoctorId(c.getDoctorID());
        vo.setShowNo(c.getShowNo() != null ? c.getShowNo() : "");
        vo.setXh(c.getXh() != null ? c.getXh() : 0);
        String rawName = c.getPatientName() != null ? c.getPatientName() : "";
        vo.setPatientName(desensitize ? DesensitizeUtil.desensitizeForClinicDisplay(rawName) : rawName);
        vo.setTriageState(c.getTriageState());
        vo.setState(c.getState());
        return vo;
    }
}
