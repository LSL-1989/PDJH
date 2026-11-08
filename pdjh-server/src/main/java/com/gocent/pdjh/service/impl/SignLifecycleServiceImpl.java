package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.constant.TerminalConstants;
import com.gocent.pdjh.dto.SignDeviceOutput;
import com.gocent.pdjh.entity.SignDevice;
import com.gocent.pdjh.repository.SignDeviceRepository;
import com.gocent.pdjh.repository.SignRoomMappingRepository;
import com.gocent.pdjh.service.SignLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/** sign_device 配置状态与终端连接状态（last_seen_at） */
@Service
@RequiredArgsConstructor
public class SignLifecycleServiceImpl implements SignLifecycleService {

    private static final long ONLINE_WINDOW_MS = 5 * 60 * 1000L;

    private final SignDeviceRepository signDeviceRepository;
    private final SignRoomMappingRepository signRoomMappingRepository;

    @Override
    public boolean isBindingComplete(String signDeviceId) {
        if (signDeviceId == null || signDeviceId.isBlank()) {
            return false;
        }
        return !signRoomMappingRepository.findBySignDeviceId(signDeviceId).isEmpty();
    }

    @Override
    public boolean isConfigComplete(SignDevice device) {
        if (device == null || device.getId() == null) {
            return false;
        }
        return isBindingComplete(device.getId());
    }

    @Override
    public void applyLifecycle(SignDevice device, String lifecycleAction) {
        if (device == null) {
            return;
        }
        if (lifecycleAction != null && "DISABLE".equalsIgnoreCase(lifecycleAction.trim())) {
            device.setStatus(TerminalConstants.SIGN_STATUS_DISABLED);
            return;
        }
        if (lifecycleAction != null && "ENABLE".equalsIgnoreCase(lifecycleAction.trim())) {
            device.setStatus(computeActiveState(device));
            return;
        }
        if (device.getStatus() != null && device.getStatus() == TerminalConstants.SIGN_STATUS_DISABLED) {
            return;
        }
        device.setStatus(computeActiveState(device));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshAfterBinding(String signDeviceId) {
        SignDevice device = signDeviceRepository.findById(signDeviceId).orElse(null);
        if (device == null) {
            return;
        }
        applyLifecycle(device, null);
        signDeviceRepository.save(device);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void touchLastSeen(String signDeviceId) {
        if (signDeviceId == null || signDeviceId.isBlank()) {
            return;
        }
        signDeviceRepository.findById(signDeviceId).ifPresent(device -> {
            device.setLastSeenAt(new Date());
            signDeviceRepository.save(device);
        });
    }

    @Override
    public boolean isOnline(SignDevice device) {
        if (device == null || device.getLastSeenAt() == null) {
            return false;
        }
        return System.currentTimeMillis() - device.getLastSeenAt().getTime() <= ONLINE_WINDOW_MS;
    }

    @Override
    public void enrichOutput(SignDeviceOutput output, SignDevice device) {
        if (output == null || device == null) {
            return;
        }
        boolean configComplete = isConfigComplete(device);
        output.setConfigComplete(configComplete);
        output.setOnline(isOnline(device));
        output.setLastSeenAt(device.getLastSeenAt());
        output.setStatusModel(formatStatusModel(device.getStatus()));
    }

    private int computeActiveState(SignDevice device) {
        return isConfigComplete(device)
                ? TerminalConstants.SIGN_STATUS_ACTIVE
                : TerminalConstants.SIGN_STATUS_PENDING;
    }

    private static String formatStatusModel(Integer status) {
        if (status != null && status == TerminalConstants.SIGN_STATUS_ACTIVE) {
            return "启用";
        }
        if (status != null && status == TerminalConstants.SIGN_STATUS_PENDING) {
            return "待配置";
        }
        return "停用";
    }
}
