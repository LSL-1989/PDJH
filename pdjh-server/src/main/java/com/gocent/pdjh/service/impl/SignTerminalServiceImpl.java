package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.constant.TerminalConstants;
import com.gocent.pdjh.dto.SignDeviceConfigOutput;
import com.gocent.pdjh.dto.terminal.TerminalRegisterOutput;
import com.gocent.pdjh.dto.terminal.TerminalResolveOutput;
import com.gocent.pdjh.entity.SignDevice;
import com.gocent.pdjh.repository.SignDeviceRepository;
import com.gocent.pdjh.repository.SignRoomMappingRepository;
import com.gocent.pdjh.service.SignLifecycleService;
import com.gocent.pdjh.service.SignTerminalService;
import com.gocent.pdjh.service.TerminalBindingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignTerminalServiceImpl implements SignTerminalService {

    private final SignDeviceRepository signDeviceRepository;
    private final SignRoomMappingRepository signRoomMappingRepository;
    private final TerminalBindingService terminalBindingService;
    private final SignLifecycleService signLifecycleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TerminalRegisterOutput register(String deviceCode) {
        String code = deviceCode.trim();
        var existing = signDeviceRepository.findByDeviceCode(code);
        if (existing.isPresent()) {
            SignDevice device = existing.get();
            signLifecycleService.touchLastSeen(device.getId());
            TerminalRegisterOutput out = new TerminalRegisterOutput();
            out.setRecordId(device.getId());
            out.setDeviceCode(device.getDeviceCode());
            out.setTerminalType(TerminalConstants.TYPE_CHECKIN);
            out.setLifecycleState(device.getStatus());
            out.setCreated(false);
            return out;
        }

        SignDevice device = new SignDevice();
        device.setId(UUID.randomUUID().toString());
        device.setDeviceCode(code);
        device.setName("待配置-" + code.substring(0, Math.min(8, code.length())));
        device.setTitle("自助签到");
        device.setStatus(TerminalConstants.SIGN_STATUS_PENDING);
        device.setCreateDate(new Date());
        signDeviceRepository.save(device);
        signLifecycleService.touchLastSeen(device.getId());
        log.info("终端自助登记 sign_device: {}", code);

        TerminalRegisterOutput out = new TerminalRegisterOutput();
        out.setRecordId(device.getId());
        out.setDeviceCode(device.getDeviceCode());
        out.setTerminalType(TerminalConstants.TYPE_CHECKIN);
        out.setLifecycleState(device.getStatus());
        out.setCreated(true);
        return out;
    }

    @Override
    public TerminalResolveOutput resolve(String deviceCode) {
        TerminalResolveOutput out = new TerminalResolveOutput();
        out.setDeviceCode(deviceCode);
        out.setTerminalType(TerminalConstants.TYPE_CHECKIN);

        SignDevice device = signDeviceRepository.findByDeviceCode(deviceCode.trim()).orElse(null);
        if (device == null) {
            out.setStatus(TerminalConstants.STATUS_NOT_REGISTERED);
            return out;
        }

        out.setRecordId(device.getId());
        out.setSignDeviceId(device.getId());
        signLifecycleService.touchLastSeen(device.getId());

        if (device.getStatus() != null && device.getStatus() == TerminalConstants.SIGN_STATUS_DISABLED) {
            out.setStatus(TerminalConstants.STATUS_FROZEN);
            return out;
        }

        if (device.getStatus() != null && device.getStatus() == TerminalConstants.SIGN_STATUS_PENDING) {
            out.setStatus(TerminalConstants.STATUS_PENDING);
            return out;
        }

        List<String> roomIds = signRoomMappingRepository.findBySignDeviceId(device.getId()).stream()
                .map(m -> m.getOfficeRoomId())
                .collect(Collectors.toList());
        if (roomIds.isEmpty()) {
            out.setStatus(TerminalConstants.STATUS_PENDING);
            return out;
        }

        if (device.getStatus() == null || device.getStatus() == TerminalConstants.SIGN_STATUS_PENDING) {
            signLifecycleService.refreshAfterBinding(device.getId());
        }

        out.setStatus(TerminalConstants.STATUS_READY);
        out.setTitle(device.getTitle());
        SignDeviceConfigOutput config = terminalBindingService.buildSignDeviceConfig(device, roomIds);
        out.setBoundRoomSummary(config.getBoundRoomSummary());
        return out;
    }
}
