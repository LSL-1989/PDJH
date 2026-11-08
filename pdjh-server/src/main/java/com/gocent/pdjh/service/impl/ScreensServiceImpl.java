package com.gocent.pdjh.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gocent.pdjh.constant.TerminalConstants;
import com.gocent.pdjh.dto.ScreensInput;
import com.gocent.pdjh.dto.ScreensOutput;
import com.gocent.pdjh.entity.DocScreen;
import com.gocent.pdjh.entity.SysConfig;
import com.gocent.pdjh.repository.DocScreenRepository;
import com.gocent.pdjh.repository.SignDeviceRepository;
import com.gocent.pdjh.service.ScreenLifecycleService;
import com.gocent.pdjh.service.ScreenRoomMappingService;
import com.gocent.pdjh.service.ScreensService;
import com.gocent.pdjh.service.SysConfigService;
import com.gocent.pdjh.service.VoiceDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreensServiceImpl implements ScreensService {

    private static final Pattern DEVICE_CODE_PATTERN = Pattern.compile("^[A-Z0-9-]+$");

    private final DocScreenRepository docScreenRepository;
    private final com.gocent.pdjh.repository.ScreenDeptMappingRepository screenDeptMappingRepository;
    private final com.gocent.pdjh.repository.DepartmentRepository departmentRepository;
    private final com.gocent.pdjh.repository.OfficeRoomRepository officeRoomRepository;
    private final ScreenRoomMappingService screenRoomMappingService;
    private final SysConfigService sysConfigService;
    private final ObjectMapper objectMapper;
    private final ScreenLifecycleService screenLifecycleService;
    private final SignDeviceRepository signDeviceRepository;
    private final VoiceDeviceService voiceDeviceService;

    private static final String SCREEN_CFG_PREFIX = TerminalConstants.SCREEN_CFG_PREFIX;
    private static final String CLINIC_CFG_PREFIX = TerminalConstants.CLINIC_CFG_PREFIX;

    /** 附加后台显示配置：SCREEN → SCREEN_CFG_*；CLINIC → CLINIC_CFG_* */
    private void attachContentConfig(ScreensOutput output) {
        if (output == null || output.getId() == null || output.getId().isBlank()) {
            return;
        }
        String prefix = TerminalConstants.TYPE_CLINIC.equals(output.getTerminalType())
                ? CLINIC_CFG_PREFIX
                : SCREEN_CFG_PREFIX;
        SysConfig row = sysConfigService.getConfigByKey(prefix + output.getId());
        if (row == null || row.getConfigValue() == null || row.getConfigValue().isBlank()) {
            return;
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(
                    row.getConfigValue(), new TypeReference<Map<String, Object>>() {});
            output.setContentConfig(parsed);
        } catch (Exception e) {
            log.warn("{} 解析失败 screenId={}: {}", prefix, output.getId(), e.getMessage());
        }
    }

    private void populateScreenBindings(ScreensOutput output) {
        if (output == null || output.getId() == null) return;
        attachVoiceBinding(output);
        String roomInfo = screenRoomMappingService.buildRoomInfoString(output.getId());
        if (roomInfo != null && !roomInfo.isBlank()) {
            output.setRoomInfo(roomInfo);
            output.setDeptType(1);
            return;
        }
        populateDeptTypeLegacy(output);
    }

    private void attachVoiceBinding(ScreensOutput output) {
        if (output.getVoiceChannel() == null || output.getVoiceChannel().isBlank()) {
            output.setVoiceChannel(VoiceDeviceServiceImpl.CHANNEL_VOICE);
        } else {
            output.setVoiceChannel(VoiceDeviceServiceImpl.normalizeChannel(output.getVoiceChannel()));
        }
        List<String> voiceIds = voiceDeviceService.listVoiceDeviceIdsForScreen(output.getId());
        output.setVoiceDeviceIds(voiceIds);
        output.setVoiceBoundSummary(voiceDeviceService.summarizeVoiceBinding(output.getId()));
    }

    private void populateDeptTypeLegacy(ScreensOutput output) {
        List<com.gocent.pdjh.entity.ScreenDeptMapping> mappings = screenDeptMappingRepository.findByScreenId(output.getId());
        if (mappings != null && !mappings.isEmpty()) {
            StringBuilder deptInfoBuilder = new StringBuilder();
            Integer determinedType = null;
            for (com.gocent.pdjh.entity.ScreenDeptMapping map : mappings) {
                com.gocent.pdjh.entity.Department dept = departmentRepository.findById(map.getDeptId()).orElse(null);
                if (dept != null) {
                    if (determinedType == null && dept.getType() != null) {
                        determinedType = dept.getType();
                    }
                    if (deptInfoBuilder.length() > 0) {
                        deptInfoBuilder.append(";");
                    }
                    deptInfoBuilder.append(dept.getId()).append("|").append(dept.getName());
                }
            }
            output.setDeptInfo(deptInfoBuilder.toString());
            // 设定类型
            output.setDeptType(determinedType != null ? determinedType : 1);
        }
    }

    @Override
    public List<ScreensOutput> getScreens(ScreensInput input) {
        String queryName = input != null ? input.getName() : null;
        String terminalTypeFilter = input != null && input.getTerminalType() != null
                ? input.getTerminalType().trim().toUpperCase() : null;
        List<DocScreen> screens = docScreenRepository.findScreensByCondition(queryName);

        return screens.stream()
                .filter(screen -> {
                    if (terminalTypeFilter == null || terminalTypeFilter.isEmpty()) {
                        return true;
                    }
                    String tt = screen.getTerminalType() != null ? screen.getTerminalType() : TerminalConstants.TYPE_SCREEN;
                    return terminalTypeFilter.equals(tt);
                })
                .map(screen -> {
            ScreensOutput output = new ScreensOutput();
            BeanUtils.copyProperties(screen, output);
            populateScreenBindings(output);
            screenLifecycleService.enrichOutput(output, screen);
            return output;
        }).collect(Collectors.toList());
    }

    @Override
    public ScreensOutput getScreensSingle(String id) {
        DocScreen screen = docScreenRepository.findById(id).orElse(null);
        if (screen == null) return null;

        ScreensOutput output = new ScreensOutput();
        BeanUtils.copyProperties(screen, output);
        populateScreenBindings(output);
        attachContentConfig(output);
        screenLifecycleService.enrichOutput(output, screen);
        return output;
    }

    private void applyInputToScreen(ScreensInput input, DocScreen screen) {
        screen.setName(input.getName());
        screen.setCallCode(input.getCallCode());
        screen.setCallNum(input.getCallNum());
        screen.setCallContent(input.getCallContent());
        screen.setMaxNum(input.getMaxNum());
        screen.setBulletin(input.getBulletin());
        screen.setScrollTime(input.getScrollTime());
        screen.setSubtitle(input.getSubtitle());
        screen.setRate(input.getRate());
        screen.setDataMode(input.getDataMode());
        screen.setIsNum(input.getIsNum());
        screen.setIsAudio(input.getIsAudio());
        if (input.getVoiceChannel() != null) {
            screen.setVoiceChannel(VoiceDeviceServiceImpl.normalizeChannel(input.getVoiceChannel()));
        } else if (screen.getVoiceChannel() == null || screen.getVoiceChannel().isBlank()) {
            screen.setVoiceChannel(VoiceDeviceServiceImpl.CHANNEL_VOICE);
        }
        String terminalType = input.getTerminalType() != null ? input.getTerminalType().trim().toUpperCase() : "";
        screen.setTerminalType(terminalType.isEmpty() ? TerminalConstants.TYPE_SCREEN : terminalType);
        if (TerminalConstants.TYPE_CLINIC.equals(screen.getTerminalType())) {
            screen.setTemplateRoute(null);
            if (input.getMaxNum() == null || input.getMaxNum() > 1) {
                screen.setMaxNum(1);
            }
        } else {
            String route = input.getTemplateRoute();
            screen.setTemplateRoute(
                    route != null && !route.isBlank() ? route : TerminalConstants.DEFAULT_TEMPLATE_ROUTE);
        }
        if (input.getStartTime() != null && !input.getStartTime().isBlank()) {
            screen.setStartTime(parseDate(input.getStartTime()));
        }
        if (input.getEndTime() != null && !input.getEndTime().isBlank()) {
            screen.setEndTime(parseDate(input.getEndTime()));
        }
    }

    private static java.util.Date parseDate(String raw) {
        String[] patterns = { "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd" };
        for (String p : patterns) {
            try {
                return new java.text.SimpleDateFormat(p).parse(raw.trim());
            } catch (Exception ignored) { }
        }
        return null;
    }

    /** 写入 deviceCode：可留空；更新时已有编号不可改，空则可首次填写 */
    private void applyDeviceCodeFromInput(ScreensInput input, DocScreen screen, String lockedDeviceCode) {
        String incoming = normalizeDeviceCode(input.getDeviceCode());
        if (!isBlank(lockedDeviceCode)) {
            String locked = normalizeDeviceCode(lockedDeviceCode);
            if (!incoming.isEmpty() && !incoming.equals(locked)) {
                throw new IllegalArgumentException("已登记屏幕的设备编号不可修改");
            }
            screen.setDeviceCode(locked);
            return;
        }
        if (incoming.isEmpty()) {
            screen.setDeviceCode(null);
            return;
        }
        assertDeviceCodeAvailable(incoming, screen.getId());
        screen.setDeviceCode(incoming);
    }

    private void assertDeviceCodeAvailable(String deviceCode, String screenId) {
        if (!DEVICE_CODE_PATTERN.matcher(deviceCode).matches()) {
            throw new IllegalArgumentException("deviceCode 仅允许数字、大写字母和连字符(-)");
        }
        docScreenRepository.findByDeviceCode(deviceCode).ifPresent(other -> {
            if (screenId == null || !screenId.equals(other.getId())) {
                throw new IllegalArgumentException("deviceCode 已存在：" + deviceCode);
            }
        });
        signDeviceRepository.findByDeviceCode(deviceCode).ifPresent(other -> {
            throw new IllegalArgumentException("deviceCode 已被签到终端占用：" + deviceCode);
        });
    }

    private static String normalizeDeviceCode(String deviceCode) {
        if (deviceCode == null) {
            return "";
        }
        return deviceCode.trim().toUpperCase();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    public boolean addScreen(ScreensInput input) {
        try {
            DocScreen screen = new DocScreen();
            if (input.getId() != null && !input.getId().isEmpty()) {
                screen.setId(input.getId());
            } else {
                screen.setId(UUID.randomUUID().toString());
            }
            applyInputToScreen(input, screen);
            applyDeviceCodeFromInput(input, screen, null);
            if (screen.getCreateDate() == null) {
                screen.setCreateDate(new java.util.Date());
            }
            screenLifecycleService.applyLifecycle(screen, null);
            docScreenRepository.save(screen);
            applyVoiceBindingFromInput(input, screen);
            log.info("添加屏幕成功: {}", screen.getName());
            return true;
        } catch (IllegalArgumentException e) {
            log.warn("添加屏幕参数无效: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("添加屏幕失败", e);
            return false;
        }
    }

    @Override
    public boolean updateScreen(ScreensInput input) {
        if (input.getId() == null) return false;

        DocScreen existing = docScreenRepository.findById(input.getId()).orElse(null);
        if (existing == null) return false;

        String lifecycleAction = input.getLifecycleAction();
        // 仅传 lifecycleAction（无业务字段）时只改状态，避免把 name/bulletin 等刷成 null
        boolean lifecycleOnly = lifecycleAction != null && !lifecycleAction.isBlank()
                && input.getName() == null
                && input.getCallCode() == null
                && input.getBulletin() == null;
        if (lifecycleOnly) {
            screenLifecycleService.applyLifecycle(existing, lifecycleAction);
            docScreenRepository.save(existing);
            log.info("屏幕生命周期更新: id={}, action={}", existing.getId(), lifecycleAction);
            return true;
        }

        String lockedDeviceCode = existing.getDeviceCode();
        applyInputToScreen(input, existing);
        applyDeviceCodeFromInput(input, existing, lockedDeviceCode);
        screenLifecycleService.applyLifecycle(existing, lifecycleAction);
        docScreenRepository.save(existing);
        applyVoiceBindingFromInput(input, existing);
        log.info("更新屏幕成功: {}", existing.getName());
        return true;
    }

    /** voiceDeviceIds 非 null 时重写本屏音响绑定；null 表示本次不改绑定 */
    private void applyVoiceBindingFromInput(ScreensInput input, DocScreen screen) {
        if (input == null || screen == null || screen.getId() == null) return;
        if (input.getVoiceDeviceIds() == null) return;
        if (TerminalConstants.TYPE_CLINIC.equals(screen.getTerminalType())) {
            // 门边屏不绑候诊区音响
            voiceDeviceService.bindVoiceDevicesToScreen(screen.getId(), List.of());
            return;
        }
        voiceDeviceService.bindVoiceDevicesToScreen(screen.getId(), input.getVoiceDeviceIds());
    }

    @Override
    public boolean delScreen(String id) {
        try {
            if (id == null || id.isBlank()) return false;
            voiceDeviceService.bindVoiceDevicesToScreen(id, List.of());
            screenRoomMappingService.relevancyScreenAndRooms(id, List.of());
            screenDeptMappingRepository.deleteByScreenId(id);
            officeRoomRepository.findAll().stream()
                    .filter(r -> id.equals(r.getScreenId()))
                    .forEach(r -> {
                        r.setScreenId(null);
                        officeRoomRepository.save(r);
                    });
            docScreenRepository.deleteById(id);
            log.info("删除屏幕成功并已解绑诊室: {}", id);
            return true;
        } catch (Exception e) {
            log.error("删除屏幕失败", e);
            return false;
        }
    }

    @Override
    public ScreensOutput getScreenByDeviceCode(String deviceCode) {
        DocScreen screen = docScreenRepository.findByDeviceCode(deviceCode).orElse(null);
        if (screen == null) return null;

        ScreensOutput output = new ScreensOutput();
        BeanUtils.copyProperties(screen, output);
        populateScreenBindings(output);
        attachContentConfig(output);
        screenLifecycleService.enrichOutput(output, screen);
        return output;
    }
}
