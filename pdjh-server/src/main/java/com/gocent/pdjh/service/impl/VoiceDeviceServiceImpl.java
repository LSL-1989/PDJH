package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.dto.VoiceDeviceInput;
import com.gocent.pdjh.dto.VoiceDeviceOutput;
import com.gocent.pdjh.dto.VoiceScopeMappingInput;
import com.gocent.pdjh.entity.DocScreen;
import com.gocent.pdjh.entity.OfficeRoom;
import com.gocent.pdjh.entity.ScreenRoomMapping;
import com.gocent.pdjh.entity.VoiceDevice;
import com.gocent.pdjh.entity.VoiceScopeMapping;
import com.gocent.pdjh.repository.DocScreenRepository;
import com.gocent.pdjh.repository.OfficeRoomRepository;
import com.gocent.pdjh.repository.ScreenRoomMappingRepository;
import com.gocent.pdjh.repository.VoiceDeviceRepository;
import com.gocent.pdjh.repository.VoiceScopeMappingRepository;
import com.gocent.pdjh.service.VoiceDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceDeviceServiceImpl implements VoiceDeviceService {

    public static final String SCOPE_ROOM = "ROOM";
    public static final String SCOPE_DEPT = "DEPT";
    /** 候诊大屏 ↔ 音响（推荐） */
    public static final String SCOPE_SCREEN = "SCREEN";

    public static final String CHANNEL_VOICE = "VOICE";
    public static final String CHANNEL_SCREEN = "SCREEN";
    public static final String CHANNEL_BOTH = "BOTH";

    public static final String DEFAULT_CALL_CONTENT = "请{0}{1}，到{2}检查";

    private final VoiceDeviceRepository voiceDeviceRepository;
    private final VoiceScopeMappingRepository voiceScopeMappingRepository;
    private final OfficeRoomRepository officeRoomRepository;
    private final DocScreenRepository docScreenRepository;
    private final ScreenRoomMappingRepository screenRoomMappingRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public List<VoiceDeviceOutput> listAll() {
        return voiceDeviceRepository.findAll().stream().map(this::toOutput).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoiceDevice save(VoiceDeviceInput input) {
        if (input == null || input.getDeviceCode() == null || input.getDeviceCode().isBlank()) {
            throw new IllegalArgumentException("设备编号不能为空");
        }
        String code = input.getDeviceCode().trim().toUpperCase();
        VoiceDevice device;
        if (input.getId() != null && !input.getId().isBlank()) {
            device = voiceDeviceRepository.findById(input.getId().trim())
                    .orElseThrow(() -> new IllegalArgumentException("音响设备不存在"));
            if (!code.equalsIgnoreCase(device.getDeviceCode())
                    && voiceDeviceRepository.existsByDeviceCode(code)) {
                throw new IllegalArgumentException("设备编号已存在：" + code);
            }
        } else {
            if (voiceDeviceRepository.existsByDeviceCode(code)) {
                throw new IllegalArgumentException("设备编号已存在：" + code);
            }
            device = new VoiceDevice();
            device.setId(UUID.randomUUID().toString().replace("-", ""));
            device.setCreateDate(new Date());
        }
        device.setDeviceCode(code);
        device.setName(input.getName());
        device.setLocation(input.getLocation());

        // 不再把单一 screenId 写回设备；绑定以映射表为准（大屏端维护）
        String migrateScreenId = input.getScreenId() != null ? input.getScreenId().trim() : "";
        if (!migrateScreenId.isEmpty()) {
            docScreenRepository.findById(migrateScreenId)
                    .orElseThrow(() -> new IllegalArgumentException("绑定的候诊大屏不存在"));
        }
        device.setScreenId(null);

        String mode = input.getPlayMode() != null ? input.getPlayMode().trim().toUpperCase() : "WS";
        if (!List.of("WS", "HTTP", "BOTH").contains(mode)) {
            mode = "WS";
        }
        device.setPlayMode(mode);
        device.setEndpointUrl(input.getEndpointUrl());
        if (("HTTP".equals(mode) || "BOTH".equals(mode))
                && (device.getEndpointUrl() == null || device.getEndpointUrl().isBlank())) {
            throw new IllegalArgumentException("HTTP/BOTH 模式请填写推送地址");
        }
        device.setState(input.getState() != null ? input.getState() : 1);
        VoiceDevice saved = voiceDeviceRepository.save(device);
        if (!migrateScreenId.isEmpty()) {
            ensureScreenMapping(saved.getId(), migrateScreenId);
        }
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean remove(String id) {
        if (id == null || id.isBlank()) return false;
        if (!voiceDeviceRepository.existsById(id)) return false;
        voiceScopeMappingRepository.deleteByVoiceDeviceId(id);
        voiceDeviceRepository.deleteById(id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindScopes(VoiceScopeMappingInput input) {
        // 兼容旧接口：仍可写入诊室/诊区映射
        if (input == null || input.getVoiceDeviceId() == null || input.getVoiceDeviceId().isBlank()) {
            throw new IllegalArgumentException("voiceDeviceId 不能为空");
        }
        String deviceId = input.getVoiceDeviceId().trim();
        if (!voiceDeviceRepository.existsById(deviceId)) {
            throw new IllegalArgumentException("音响设备不存在");
        }
        // 只清 ROOM/DEPT，保留 SCREEN 映射
        List<VoiceScopeMapping> keep = voiceScopeMappingRepository.findByVoiceDeviceId(deviceId).stream()
                .filter(m -> SCOPE_SCREEN.equals(m.getScopeType()))
                .collect(Collectors.toList());
        voiceScopeMappingRepository.deleteByVoiceDeviceId(deviceId);
        List<VoiceScopeMapping> rows = new ArrayList<>(keep);
        if (input.getOfficeRoomIds() != null) {
            for (String roomId : input.getOfficeRoomIds()) {
                if (roomId == null || roomId.isBlank()) continue;
                VoiceScopeMapping m = new VoiceScopeMapping();
                m.setVoiceDeviceId(deviceId);
                m.setScopeType(SCOPE_ROOM);
                m.setScopeId(roomId.trim());
                rows.add(m);
            }
        }
        if (input.getDeptIds() != null) {
            for (String deptId : input.getDeptIds()) {
                if (deptId == null || deptId.isBlank()) continue;
                VoiceScopeMapping m = new VoiceScopeMapping();
                m.setVoiceDeviceId(deviceId);
                m.setScopeType(SCOPE_DEPT);
                m.setScopeId(deptId.trim());
                rows.add(m);
            }
        }
        if (!rows.isEmpty()) {
            voiceScopeMappingRepository.saveAll(rows);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindVoiceDevicesToScreen(String screenId, List<String> voiceDeviceIds) {
        if (screenId == null || screenId.isBlank()) {
            throw new IllegalArgumentException("screenId 不能为空");
        }
        String sid = screenId.trim();
        if (!docScreenRepository.existsById(sid)) {
            throw new IllegalArgumentException("候诊大屏不存在");
        }
        voiceScopeMappingRepository.deleteByScopeTypeAndScopeId(SCOPE_SCREEN, sid);
        // 同步清掉旧版 voice_device.screen_id 指向本屏的残留
        for (VoiceDevice d : voiceDeviceRepository.findByScreenId(sid)) {
            d.setScreenId(null);
            voiceDeviceRepository.save(d);
        }
        if (voiceDeviceIds == null || voiceDeviceIds.isEmpty()) {
            return;
        }
        List<VoiceScopeMapping> rows = new ArrayList<>();
        for (String rawId : voiceDeviceIds) {
            if (rawId == null || rawId.isBlank()) continue;
            String deviceId = rawId.trim();
            if (!voiceDeviceRepository.existsById(deviceId)) {
                throw new IllegalArgumentException("音响设备不存在：" + deviceId);
            }
            VoiceScopeMapping m = new VoiceScopeMapping();
            m.setVoiceDeviceId(deviceId);
            m.setScopeType(SCOPE_SCREEN);
            m.setScopeId(sid);
            rows.add(m);
        }
        if (!rows.isEmpty()) {
            voiceScopeMappingRepository.saveAll(rows);
        }
    }

    @Override
    public List<String> listVoiceDeviceIdsForScreen(String screenId) {
        if (screenId == null || screenId.isBlank()) return List.of();
        String sid = screenId.trim();
        LinkedHashMap<String, Boolean> ids = new LinkedHashMap<>();
        for (VoiceScopeMapping m : voiceScopeMappingRepository.findByScopeTypeAndScopeId(SCOPE_SCREEN, sid)) {
            if (m.getVoiceDeviceId() != null && !m.getVoiceDeviceId().isBlank()) {
                ids.putIfAbsent(m.getVoiceDeviceId().trim(), Boolean.TRUE);
            }
        }
        // 兼容旧版 voice_device.screen_id
        for (VoiceDevice d : voiceDeviceRepository.findByScreenId(sid)) {
            ids.putIfAbsent(d.getId(), Boolean.TRUE);
        }
        return new ArrayList<>(ids.keySet());
    }

    @Override
    public String summarizeVoiceBinding(String screenId) {
        List<String> ids = listVoiceDeviceIdsForScreen(screenId);
        if (ids.isEmpty()) return "未绑定音响";
        List<String> names = new ArrayList<>();
        for (String id : ids) {
            voiceDeviceRepository.findById(id).ifPresent(d ->
                    names.add(d.getName() != null && !d.getName().isBlank() ? d.getName() : d.getDeviceCode()));
        }
        return names.isEmpty() ? ("已绑 " + ids.size() + " 台") : String.join("、", names);
    }

    @Override
    public VoiceDeviceOutput getByDeviceCode(String deviceCode) {
        if (deviceCode == null || deviceCode.isBlank()) {
            throw new IllegalArgumentException("deviceCode 不能为空");
        }
        VoiceDevice device = voiceDeviceRepository.findByDeviceCode(deviceCode.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("音响设备不存在或未登记"));
        if (device.getState() != null && device.getState() == 0) {
            throw new IllegalArgumentException("音响设备已停用");
        }
        return toOutput(device);
    }

    @Override
    public String formatCallText(String officeRoomId, String callCode, String showNo, String patientName, String office) {
        DocScreen screen = resolvePrimaryScreen(officeRoomId, callCode);
        String template = screen != null ? screen.getCallContent() : null;
        if (screen != null) {
            log.info("叫号文案模板来源: screen={} ({}) template={}",
                    screen.getName(), screen.getId(), template);
        } else {
            log.info("叫号文案模板来源: 默认（诊室未关联候诊大屏） officeRoomId={} callCode={}",
                    officeRoomId, callCode);
        }
        return applyCallContent(template, showNo, patientName, office);
    }

    @Override
    public String resolveVoiceChannel(String officeRoomId, String callCode) {
        DocScreen screen = resolvePrimaryScreen(officeRoomId, callCode);
        return normalizeChannel(screen != null ? screen.getVoiceChannel() : null);
    }

    @Override
    public String resolveSpeakScreenId(String officeRoomId, String callCode) {
        DocScreen screen = resolvePrimaryScreen(officeRoomId, callCode);
        return screen != null ? screen.getId() : null;
    }

    @Override
    public Map<String, Object> resolveSpeakOpts(String officeRoomId, String callCode) {
        return withScreenVoiceOpts(resolvePrimaryScreen(officeRoomId, callCode), null);
    }

    @Override
    public int broadcastCall(String officeRoomId, String showNo, String patientName, String office,
                             Map<String, Object> extra) {
        List<DocScreen> screens = resolveScreensForRoom(officeRoomId, null);
        Map<String, String> screenIdToText = new LinkedHashMap<>();
        for (DocScreen screen : screens) {
            screenIdToText.put(screen.getId(),
                    applyCallContent(screen.getCallContent(), showNo, patientName, office));
        }

        Map<String, VoiceDevice> devices = new LinkedHashMap<>();

        // 1) 大屏端绑定的音响（SCREEN 映射 + 旧 screen_id）
        for (DocScreen screen : screens) {
            String text = screenIdToText.get(screen.getId());
            Map<String, Object> opts = withScreenVoiceOpts(screen, extra);
            for (VoiceDevice d : resolveDevicesForScreen(screen.getId())) {
                if (d.getState() != null && d.getState() == 0) continue;
                if (devices.putIfAbsent(d.getId(), d) == null) {
                    pushToDevice(d, text, opts);
                }
            }
        }

        // 2) 兼容旧诊室/诊区绑定
        DocScreen preferred = preferWaitingHallScreen(screens);
        String fallbackText = preferred != null
                ? applyCallContent(preferred.getCallContent(), showNo, patientName, office)
                : applyCallContent(null, showNo, patientName, office);
        Map<String, Object> fallbackOpts = withScreenVoiceOpts(preferred, extra);

        for (VoiceDevice legacy : resolveLegacyDevicesForRoom(officeRoomId)) {
            if (devices.containsKey(legacy.getId())) continue;
            String text = fallbackText;
            Map<String, Object> opts = fallbackOpts;
            if (legacy.getScreenId() != null && !legacy.getScreenId().isBlank()) {
                text = screenIdToText.getOrDefault(legacy.getScreenId(), fallbackText);
                DocScreen legacyScreen = null;
                if (screenIdToText.containsKey(legacy.getScreenId())) {
                    for (DocScreen s : screens) {
                        if (legacy.getScreenId().equals(s.getId())) {
                            legacyScreen = s;
                            break;
                        }
                    }
                } else {
                    legacyScreen = docScreenRepository.findById(legacy.getScreenId()).orElse(null);
                    if (legacyScreen != null) {
                        text = applyCallContent(legacyScreen.getCallContent(), showNo, patientName, office);
                    }
                }
                if (legacyScreen != null) {
                    opts = withScreenVoiceOpts(legacyScreen, extra);
                }
            }
            devices.put(legacy.getId(), legacy);
            pushToDevice(legacy, text, opts);
        }

        if (devices.isEmpty()) {
            log.debug("诊室 {} 无绑定独立音响，跳过推送", officeRoomId);
        }
        return devices.size();
    }

    /** 把候诊大屏的呼叫次数、语速写入音响推送载荷（音响终端按此播报）。 */
    static Map<String, Object> withScreenVoiceOpts(DocScreen screen, Map<String, Object> extra) {
        Map<String, Object> m = new HashMap<>();
        if (extra != null) {
            m.putAll(extra);
        }
        int callNum = 1;
        int rate = 0;
        if (screen != null) {
            if (screen.getCallNum() != null && screen.getCallNum() > 0) {
                callNum = Math.min(5, screen.getCallNum());
            }
            if (screen.getRate() != null) {
                rate = Math.max(-10, Math.min(10, screen.getRate()));
            }
        }
        m.put("callNum", callNum);
        m.put("rate", rate);
        return m;
    }

    /** 默认模板：请{0}{1}，到{2}检查 → {0}号序 {1}姓名 {2}诊室 */
    static String applyCallContent(String template, String showNo, String patientName, String office) {
        String tpl = (template == null || template.isBlank()) ? DEFAULT_CALL_CONTENT : template.trim();
        Object[] args = {
                showNo != null ? showNo : "",
                patientName != null ? patientName : "",
                office != null ? office : ""
        };
        try {
            return MessageFormat.format(tpl.replace("'", "''"), args);
        } catch (Exception e) {
            return String.format("请%s%s，到%s", args[0], args[1], args[2]);
        }
    }

    static String normalizeChannel(String raw) {
        if (raw == null || raw.isBlank()) return CHANNEL_VOICE;
        String c = raw.trim().toUpperCase();
        if (CHANNEL_SCREEN.equals(c) || CHANNEL_BOTH.equals(c) || CHANNEL_VOICE.equals(c)) {
            return c;
        }
        return CHANNEL_VOICE;
    }

    private DocScreen resolvePrimaryScreen(String officeRoomId, String callCode) {
        return preferWaitingHallScreen(resolveScreensForRoom(officeRoomId, callCode));
    }

    /**
     * 同一诊室可能同时关联「候诊大屏(SCREEN)」与「诊室屏(CLINIC)」。
     * 语音播报以候诊大屏 CallContent / voiceChannel 为准。
     */
    private static DocScreen preferWaitingHallScreen(List<DocScreen> screens) {
        if (screens == null || screens.isEmpty()) return null;
        for (DocScreen s : screens) {
            String t = s.getTerminalType();
            if (t == null || t.isBlank() || "SCREEN".equalsIgnoreCase(t)) {
                return s;
            }
        }
        return screens.get(0);
    }

    private List<DocScreen> resolveScreensForRoom(String officeRoomId, String callCode) {
        Map<String, DocScreen> found = new LinkedHashMap<>();
        String code = callCode != null ? callCode.trim() : "";

        if (!code.isEmpty()) {
            for (DocScreen s : docScreenRepository.findAll()) {
                if (code.equals(s.getCallCode()) || code.equals(s.getId())
                        || (s.getDeviceCode() != null && code.equalsIgnoreCase(s.getDeviceCode()))) {
                    found.put(s.getId(), s);
                }
            }
        }

        if (officeRoomId != null && !officeRoomId.isBlank()) {
            String roomId = officeRoomId.trim();
            for (ScreenRoomMapping m : screenRoomMappingRepository.findByOfficeRoomId(roomId)) {
                if (m.getScreenId() == null || m.getScreenId().isBlank()) continue;
                docScreenRepository.findById(m.getScreenId().trim())
                        .ifPresent(s -> found.putIfAbsent(s.getId(), s));
            }
            officeRoomRepository.findById(roomId).ifPresent(room -> {
                if (room.getScreenId() != null && !room.getScreenId().isBlank()) {
                    docScreenRepository.findById(room.getScreenId().trim())
                            .ifPresent(s -> found.putIfAbsent(s.getId(), s));
                }
            });
        }
        return new ArrayList<>(found.values());
    }

    private List<VoiceDevice> resolveDevicesForScreen(String screenId) {
        Map<String, VoiceDevice> found = new LinkedHashMap<>();
        for (String id : listVoiceDeviceIdsForScreen(screenId)) {
            addEnabled(found, id);
        }
        return new ArrayList<>(found.values());
    }

    private List<VoiceDevice> resolveLegacyDevicesForRoom(String officeRoomId) {
        Map<String, VoiceDevice> found = new LinkedHashMap<>();
        if (officeRoomId == null || officeRoomId.isBlank()) {
            return List.of();
        }
        String roomId = officeRoomId.trim();
        OfficeRoom room = officeRoomRepository.findById(roomId).orElse(null);

        for (VoiceScopeMapping m : voiceScopeMappingRepository.findByScopeTypeAndScopeId(SCOPE_ROOM, roomId)) {
            addEnabled(found, m.getVoiceDeviceId());
        }
        if (room != null && room.getDeptId() != null && !room.getDeptId().isBlank()) {
            for (VoiceScopeMapping m : voiceScopeMappingRepository.findByScopeTypeAndScopeId(SCOPE_DEPT, room.getDeptId())) {
                addEnabled(found, m.getVoiceDeviceId());
            }
        }
        if (room != null && room.getVoiceDeviceId() != null && !room.getVoiceDeviceId().isBlank()) {
            String ref = room.getVoiceDeviceId().trim();
            voiceDeviceRepository.findById(ref)
                    .or(() -> voiceDeviceRepository.findByDeviceCode(ref.toUpperCase()))
                    .ifPresent(d -> {
                        if (d.getState() == null || d.getState() == 1) {
                            found.putIfAbsent(d.getId(), d);
                        }
                    });
        }
        return new ArrayList<>(found.values());
    }

    private void ensureScreenMapping(String voiceDeviceId, String screenId) {
        if (voiceDeviceId == null || voiceDeviceId.isBlank()) return;
        boolean exists = voiceScopeMappingRepository.findByScopeTypeAndScopeId(SCOPE_SCREEN, screenId).stream()
                .anyMatch(m -> voiceDeviceId.equals(m.getVoiceDeviceId()));
        if (exists) return;
        VoiceScopeMapping m = new VoiceScopeMapping();
        m.setVoiceDeviceId(voiceDeviceId);
        m.setScopeType(SCOPE_SCREEN);
        m.setScopeId(screenId);
        voiceScopeMappingRepository.save(m);
    }

    private void addEnabled(Map<String, VoiceDevice> found, String deviceId) {
        if (deviceId == null || deviceId.isBlank() || found.containsKey(deviceId)) return;
        voiceDeviceRepository.findById(deviceId).ifPresent(d -> {
            if (d.getState() == null || d.getState() == 1) {
                found.put(d.getId(), d);
            }
        });
    }

    private void pushToDevice(VoiceDevice device, String callText, Map<String, Object> extra) {
        if (callText == null || callText.isBlank()) return;
        String mode = device.getPlayMode() != null ? device.getPlayMode().toUpperCase() : "WS";
        Map<String, Object> payload = new HashMap<>();
        if (extra != null) payload.putAll(extra);
        payload.put("type", "VOICE_CALL");
        payload.put("deviceCode", device.getDeviceCode());
        payload.put("callText", callText);
        payload.put("ttsCallText", callText);

        if ("WS".equals(mode) || "BOTH".equals(mode)) {
            String topic = "/topic/voice/" + device.getDeviceCode();
            messagingTemplate.convertAndSend(topic, (Object) payload);
            log.info("独立音响 WS 推送 → {} : {}", topic, callText);
        }
        if ("HTTP".equals(mode) || "BOTH".equals(mode)) {
            if (device.getEndpointUrl() == null || device.getEndpointUrl().isBlank()) {
                log.warn("音响 {} 为 HTTP 模式但未配置 endpointUrl", device.getDeviceCode());
                return;
            }
            try {
                restTemplate.postForObject(device.getEndpointUrl(), payload, String.class);
                log.info("独立音响 HTTP 推送 → {} : {}", device.getEndpointUrl(), callText);
            } catch (Exception e) {
                log.warn("独立音响 HTTP 推送失败 [{}]: {}", device.getDeviceCode(), e.getMessage());
            }
        }
    }

    private VoiceDeviceOutput toOutput(VoiceDevice d) {
        VoiceDeviceOutput out = new VoiceDeviceOutput();
        out.setId(d.getId());
        out.setDeviceCode(d.getDeviceCode());
        out.setName(d.getName());
        out.setLocation(d.getLocation());
        out.setPlayMode(d.getPlayMode());
        out.setEndpointUrl(d.getEndpointUrl());
        out.setState(d.getState());
        out.setCreateDate(d.getCreateDate());

        List<String> screenNames = new ArrayList<>();
        LinkedHashMap<String, String> screenIds = new LinkedHashMap<>();
        for (VoiceScopeMapping m : voiceScopeMappingRepository.findByVoiceDeviceId(d.getId())) {
            if (SCOPE_SCREEN.equals(m.getScopeType()) && m.getScopeId() != null) {
                screenIds.putIfAbsent(m.getScopeId(), m.getScopeId());
            } else if (SCOPE_ROOM.equals(m.getScopeType())) {
                out.getOfficeRoomIds().add(m.getScopeId());
            } else if (SCOPE_DEPT.equals(m.getScopeType())) {
                out.getDeptIds().add(m.getScopeId());
            }
        }
        if (d.getScreenId() != null && !d.getScreenId().isBlank()) {
            screenIds.putIfAbsent(d.getScreenId(), d.getScreenId());
        }
        for (String sid : screenIds.keySet()) {
            docScreenRepository.findById(sid).ifPresent(s ->
                    screenNames.add(s.getName() != null ? s.getName() : s.getId()));
        }
        if (!screenIds.isEmpty()) {
            String first = screenIds.keySet().iterator().next();
            out.setScreenId(first);
            docScreenRepository.findById(first).ifPresent(s -> {
                out.setScreenName(s.getName());
                out.setCallContent(s.getCallContent());
            });
        }
        if (!screenNames.isEmpty()) {
            out.setBoundSummary("大屏：" + String.join("、", screenNames));
        } else if (!out.getOfficeRoomIds().isEmpty() || !out.getDeptIds().isEmpty()) {
            out.setBoundSummary("旧绑定 " + out.getOfficeRoomIds().size() + " 诊室 · "
                    + out.getDeptIds().size() + " 诊区");
        } else {
            out.setBoundSummary("未绑定候诊大屏（请在屏幕管理中绑定）");
        }
        return out;
    }
}
