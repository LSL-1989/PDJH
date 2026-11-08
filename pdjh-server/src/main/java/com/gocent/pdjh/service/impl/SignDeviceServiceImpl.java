package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.constant.TerminalConstants;
import com.gocent.pdjh.dto.SignDeviceConfigOutput;
import com.gocent.pdjh.dto.SignDeviceInput;
import com.gocent.pdjh.dto.SignDeviceOutput;
import com.gocent.pdjh.entity.SignDevice;
import com.gocent.pdjh.entity.SignRoomMapping;
import com.gocent.pdjh.repository.SignDeviceRepository;
import com.gocent.pdjh.repository.SignRoomMappingRepository;
import com.gocent.pdjh.service.SignDeviceService;
import com.gocent.pdjh.service.SignLifecycleService;
import com.gocent.pdjh.service.TerminalBindingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 签到终端管理：列表、保存、诊室绑定；配置状态与在线状态由 SignLifecycleService 维护 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignDeviceServiceImpl implements SignDeviceService {

    private static final Pattern DEVICE_CODE_PATTERN = Pattern.compile("^[A-Z0-9-]+$");

    private final SignDeviceRepository signDeviceRepository;
    private final SignRoomMappingRepository signRoomMappingRepository;
    private final TerminalBindingService terminalBindingService;
    private final SignLifecycleService signLifecycleService;

    @Override
    public List<SignDeviceOutput> listAll(String name) {
        List<SignDevice> devices = (name == null || name.isBlank())
                ? signDeviceRepository.findAll()
                : signDeviceRepository.findByNameContainingIgnoreCase(name.trim());
        return devices.stream().map(this::toOutput).collect(Collectors.toList());
    }

    @Override
    public SignDeviceConfigOutput getDeviceConfig(String deviceCode) {
        SignDevice device = terminalBindingService.resolveSignDevice(deviceCode);
        signLifecycleService.touchLastSeen(device.getId());
        List<String> roomIds = signRoomMappingRepository.findBySignDeviceId(device.getId()).stream()
                .map(SignRoomMapping::getOfficeRoomId)
                .collect(Collectors.toList());
        if (roomIds.isEmpty()) {
            throw new com.gocent.pdjh.exception.TerminalBindingException(403, "签到终端尚未绑定物理诊室，请在管理后台完成配置");
        }
        return terminalBindingService.buildSignDeviceConfig(device, roomIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SignDeviceOutput save(SignDeviceInput input) {
        if (input.getDeviceCode() == null || input.getDeviceCode().isBlank()) {
            throw new IllegalArgumentException("deviceCode 不能为空");
        }
        String normalizedCode = normalizeDeviceCode(input.getDeviceCode());
        if (!DEVICE_CODE_PATTERN.matcher(normalizedCode).matches()) {
            throw new IllegalArgumentException("deviceCode 仅允许数字、大写字母和连字符(-)");
        }

        SignDevice device;
        boolean isNew = input.getId() == null || input.getId().isBlank();
        if (!isNew) {
            device = signDeviceRepository.findById(input.getId())
                    .orElseThrow(() -> new IllegalArgumentException("签到终端不存在"));
            if (!normalizedCode.equals(device.getDeviceCode())) {
                throw new IllegalArgumentException("已登记设备的编号不可修改");
            }
        } else {
            signDeviceRepository.findByDeviceCode(normalizedCode).ifPresent(existing -> {
                throw new IllegalArgumentException("deviceCode 已存在：" + normalizedCode);
            });
            device = new SignDevice();
            device.setId(UUID.randomUUID().toString());
            device.setCreateDate(new Date());
            device.setDeviceCode(normalizedCode);
            device.setStatus(TerminalConstants.SIGN_STATUS_PENDING);
        }

        device.setName(input.getName());
        device.setTitle(input.getTitle());
        device.setLocation(input.getLocation());
        device.setShowText(input.getShowText());
        signLifecycleService.applyLifecycle(device, input.getLifecycleAction());
        signDeviceRepository.save(device);
        log.info("签到终端保存成功: {}", device.getDeviceCode());
        return toOutput(device);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean remove(String id) {
        signRoomMappingRepository.deleteBySignDeviceId(id);
        signDeviceRepository.deleteById(id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean relevancySignDeviceAndRooms(String signDeviceId, List<String> officeRoomIds) {
        signRoomMappingRepository.deleteBySignDeviceId(signDeviceId);
        if (officeRoomIds != null) {
            List<SignRoomMapping> mappings = officeRoomIds.stream().map(roomId -> {
                SignRoomMapping mapping = new SignRoomMapping();
                mapping.setSignDeviceId(signDeviceId);
                mapping.setOfficeRoomId(roomId);
                return mapping;
            }).collect(Collectors.toList());
            signRoomMappingRepository.saveAll(mappings);
        }
        signLifecycleService.refreshAfterBinding(signDeviceId);
        return true;
    }

    private SignDeviceOutput toOutput(SignDevice device) {
        List<String> roomIds = signRoomMappingRepository.findBySignDeviceId(device.getId()).stream()
                .map(SignRoomMapping::getOfficeRoomId)
                .collect(Collectors.toList());
        SignDeviceOutput out = new SignDeviceOutput();
        out.setId(device.getId());
        out.setDeviceCode(device.getDeviceCode());
        out.setName(device.getName());
        out.setTitle(device.getTitle());
        out.setLocation(device.getLocation());
        out.setShowText(device.getShowText());
        out.setStatus(device.getStatus());
        out.setCreateDate(device.getCreateDate());
        out.setBoundRooms(terminalBindingService.toRoomBriefs(roomIds));
        out.setBoundRoomSummary(out.getBoundRooms().stream()
                .map(r -> r.getName())
                .collect(Collectors.joining("、")));
        signLifecycleService.enrichOutput(out, device);
        return out;
    }

    private static String normalizeDeviceCode(String deviceCode) {
        return deviceCode.trim().toUpperCase();
    }
}
