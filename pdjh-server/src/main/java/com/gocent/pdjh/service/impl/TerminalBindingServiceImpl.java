package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.dto.OfficeRoomBrief;
import com.gocent.pdjh.dto.SignDeviceConfigOutput;
import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.entity.OfficeRoom;
import com.gocent.pdjh.entity.SignDevice;
import com.gocent.pdjh.exception.TerminalBindingException;
import com.gocent.pdjh.repository.CallListRepository;
import com.gocent.pdjh.repository.DepartmentRepository;
import com.gocent.pdjh.repository.OfficeRoomRepository;
import com.gocent.pdjh.repository.ScreenRoomMappingRepository;
import com.gocent.pdjh.repository.SignDeviceRepository;
import com.gocent.pdjh.repository.SignRoomMappingRepository;
import com.gocent.pdjh.service.SysConfigService;
import com.gocent.pdjh.service.TerminalBindingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TerminalBindingServiceImpl implements TerminalBindingService {

    private final SignDeviceRepository signDeviceRepository;
    private final SignRoomMappingRepository signRoomMappingRepository;
    private final ScreenRoomMappingRepository screenRoomMappingRepository;
    private final OfficeRoomRepository officeRoomRepository;
    private final CallListRepository callListRepository;
    private final DepartmentRepository departmentRepository;
    private final SysConfigService sysConfigService;

    @Override
    public SignDevice resolveSignDevice(String deviceCode) {
        if (deviceCode == null || deviceCode.isBlank()) {
            throw new TerminalBindingException(400, "设备识别码(deviceCode)不能为空");
        }
        SignDevice device = signDeviceRepository.findByDeviceCode(deviceCode.trim())
                .orElseThrow(() -> new TerminalBindingException(404, "设备未注册，请在管理后台绑定签到终端：" + deviceCode));
        if (device.getStatus() != null && device.getStatus() == 0) {
            throw new TerminalBindingException(403, "当前签到终端已被停用，请联系管理员");
        }
        if (device.getStatus() != null && device.getStatus() == 2) {
            throw new TerminalBindingException(428, "签到终端待配置，请在管理后台完成绑定");
        }
        return device;
    }

    @Override
    public List<String> resolveSignDeviceRoomIds(String deviceCode) {
        SignDevice device = resolveSignDevice(deviceCode);
        List<String> roomIds = signRoomMappingRepository.findBySignDeviceId(device.getId()).stream()
                .map(m -> m.getOfficeRoomId())
                .collect(Collectors.toList());
        if (roomIds.isEmpty()) {
            throw new TerminalBindingException(403, "签到终端尚未绑定物理诊室，请在管理后台完成配置");
        }
        return roomIds;
    }

    @Override
    public List<String> resolveScreenRoomIds(String screenId) {
        if (screenId == null || screenId.isBlank()) {
            throw new TerminalBindingException(400, "大屏ID不能为空");
        }
        List<String> roomIds = screenRoomMappingRepository.findByScreenId(screenId).stream()
                .map(m -> m.getOfficeRoomId())
                .collect(Collectors.toList());
        if (roomIds.isEmpty()) {
            throw new TerminalBindingException(403, "大屏尚未绑定物理诊室，请在管理后台完成诊室绑定");
        }
        return roomIds;
    }

    @Override
    public SignDeviceConfigOutput buildSignDeviceConfig(SignDevice device, List<String> roomIds) {
        List<OfficeRoomBrief> briefs = toRoomBriefs(roomIds);
        SignDeviceConfigOutput out = new SignDeviceConfigOutput();
        out.setDeviceCode(device.getDeviceCode());
        out.setName(device.getName());
        out.setTitle(device.getTitle());
        out.setLocation(device.getLocation());
        out.setShowText(device.getShowText());
        out.setBoundRooms(briefs);
        out.setBoundRoomSummary(briefs.stream().map(OfficeRoomBrief::getName).collect(Collectors.joining("、")));
        out.setScene(inferSignScene(briefs));
        out.setHospitalName(configValue("HospitalName"));
        out.setHospitalLogo(configValue("HospitalLogo"));
        return out;
    }

    /** 签到机场景：药房优先于检验，避免一台机绑了两类诊室时走错入队接口 */
    private static String inferSignScene(List<OfficeRoomBrief> briefs) {
        boolean pharmacy = briefs.stream().anyMatch(b ->
                b.getBusinessType() != null && "PHARMACY".equalsIgnoreCase(b.getBusinessType()));
        if (pharmacy) return "PHARMACY";
        boolean lab = briefs.stream().anyMatch(b ->
                b.getBusinessType() != null && "LAB".equalsIgnoreCase(b.getBusinessType()));
        if (lab) return "LAB";
        return "CLINIC";
    }

    /** 读取 sys_config 文本项；缺失时返回空串（与门边屏 ClinicScreenServiceImpl 一致） */
    private String configValue(String key) {
        var cfg = sysConfigService.getConfigByKey(key);
        if (cfg == null || cfg.getConfigValue() == null) {
            return "";
        }
        return cfg.getConfigValue().trim();
    }

    @Override
    public List<OfficeRoomBrief> toRoomBriefs(List<String> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return List.of();
        }
        List<OfficeRoomBrief> briefs = new ArrayList<>();
        for (String roomId : roomIds) {
            officeRoomRepository.findById(roomId).ifPresent(room -> briefs.add(toBrief(room)));
        }
        return briefs;
    }

    @Override
    public void assertRoomAllowed(String roomId, List<String> allowedRoomIds) {
        if (roomId == null || roomId.isBlank()) {
            throw new TerminalBindingException(400, "挂号信息缺少诊室，请联系分诊台");
        }
        if (allowedRoomIds == null || !allowedRoomIds.contains(roomId)) {
            CallList probe = new CallList();
            probe.setOfficeRoomId(roomId);
            throw new TerminalBindingException(403, buildWrongAreaMessage(probe));
        }
    }

    @Override
    public void assertScreenRoomBinding(String screenId) {
        resolveScreenRoomIds(screenId);
    }

    @Override
    public CallList matchPendingSignIn(String patientCode, List<String> allowedRoomIds) {
        List<CallList> unarrived = callListRepository
                .findByPatientCodeAndTriageStateAndOfficeRoomIdInOrderByIdDesc(patientCode, "UNARRIVED", allowedRoomIds);
        if (!unarrived.isEmpty()) {
            CallList match = unarrived.get(0);
            if (match.getOfficeRoomId() == null || match.getOfficeRoomId().isBlank()) {
                throw new TerminalBindingException(400, "挂号信息缺少诊室，请联系分诊台");
            }
            return match;
        }

        CallList anyUnarrived = callListRepository.findFirstByPatientCodeAndTriageStateOrderByIdDesc(patientCode, "UNARRIVED");
        if (anyUnarrived != null) {
            throw new TerminalBindingException(403, buildWrongAreaMessage(anyUnarrived));
        }

        CallList latestToday = callListRepository.findTodayLatestByPatientCode(patientCode);
        if (latestToday == null) {
            return null;
        }
        if (latestToday.getOfficeRoomId() != null && allowedRoomIds.contains(latestToday.getOfficeRoomId())) {
            return latestToday;
        }
        if (latestToday.getOfficeRoomId() != null) {
            throw new TerminalBindingException(403, buildWrongAreaMessage(latestToday));
        }
        throw new TerminalBindingException(400, "挂号信息缺少诊室，请联系分诊台");
    }

    @Override
    public String buildWrongAreaMessage(CallList callList) {
        String roomName = callList.getOutPatientOffice();
        if ((roomName == null || roomName.isBlank()) && callList.getOfficeRoomId() != null) {
            roomName = officeRoomRepository.findById(callList.getOfficeRoomId()).map(OfficeRoom::getName).orElse("指定诊室");
        }
        if (roomName == null || roomName.isBlank()) {
            roomName = "指定诊室";
        }
        String targetArea = "正确签到区域";
        if (callList.getOfficeRoomId() != null) {
            OfficeRoom room = officeRoomRepository.findById(callList.getOfficeRoomId()).orElse(null);
            if (room != null) {
                String floor = room.getFloor() != null ? room.getFloor() : "";
                targetArea = floor.isEmpty() ? room.getName() : floor + " " + room.getName();
            }
        }
        return "您的挂号诊室为【" + roomName + "】，请前往【" + targetArea + "】签到";
    }

    private OfficeRoomBrief toBrief(OfficeRoom room) {
        OfficeRoomBrief brief = new OfficeRoomBrief();
        brief.setId(room.getId());
        brief.setName(room.getName());
        brief.setFloor(room.getFloor());
        brief.setRoomNo(room.getRoomNo());
        brief.setDeptId(room.getDeptId());
        String bt = room.getBusinessType();
        if ((bt == null || bt.isBlank()) && room.getDeptId() != null) {
            bt = departmentRepository.findById(room.getDeptId())
                    .map(d -> d.getBusinessType())
                    .orElse(null);
        }
        brief.setBusinessType(bt);
        String floor = room.getFloor() != null ? room.getFloor() : "";
        brief.setLocationLabel(floor.isEmpty() ? room.getName() : floor + " " + room.getName());
        return brief;
    }
}
