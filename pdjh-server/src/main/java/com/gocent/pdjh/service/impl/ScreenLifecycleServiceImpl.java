package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.constant.TerminalConstants;
import com.gocent.pdjh.dto.ScreensOutput;
import com.gocent.pdjh.entity.DocScreen;
import com.gocent.pdjh.repository.DocScreenRepository;
import com.gocent.pdjh.repository.ScreenDeptMappingRepository;
import com.gocent.pdjh.repository.ScreenRoomMappingRepository;
import com.gocent.pdjh.service.ScreenLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/** doc_screen 配置状态与终端连接状态（last_seen_at） */
@Service
@RequiredArgsConstructor
public class ScreenLifecycleServiceImpl implements ScreenLifecycleService {

    private static final long ONLINE_WINDOW_MS = 5 * 60 * 1000L;

    private final DocScreenRepository docScreenRepository;
    private final ScreenRoomMappingRepository screenRoomMappingRepository;
    private final ScreenDeptMappingRepository screenDeptMappingRepository;

    @Override
    public boolean isBindingComplete(String screenId, String terminalType) {
        if (screenId == null || screenId.isBlank()) {
            return false;
        }
        List<?> rooms = screenRoomMappingRepository.findByScreenId(screenId);
        if (TerminalConstants.TYPE_CLINIC.equals(terminalType)) {
            return rooms.size() == 1;
        }
        if (!rooms.isEmpty()) {
            return true;
        }
        return !screenDeptMappingRepository.findByScreenId(screenId).isEmpty();
    }

    @Override
    public boolean isConfigComplete(DocScreen screen) {
        if (screen == null || screen.getId() == null) {
            return false;
        }
        if (isBlank(screen.getName()) || isBlank(screen.getCallCode())) {
            return false;
        }
        if (isBlank(screen.getDeviceCode())) {
            return false;
        }
        String terminalType = screen.getTerminalType() != null ? screen.getTerminalType() : TerminalConstants.TYPE_SCREEN;
        return isBindingComplete(screen.getId(), terminalType);
    }

    @Override
    public void applyLifecycle(DocScreen screen, String lifecycleAction) {
        if (screen == null) {
            return;
        }
        if (lifecycleAction != null && "FREEZE".equalsIgnoreCase(lifecycleAction.trim())) {
            screen.setState(TerminalConstants.SCREEN_STATE_FROZEN);
            return;
        }
        if (lifecycleAction != null && "UNFREEZE".equalsIgnoreCase(lifecycleAction.trim())) {
            screen.setState(computeActiveState(screen));
            return;
        }
        if (screen.getState() != null && screen.getState() == TerminalConstants.SCREEN_STATE_FROZEN) {
            return;
        }
        screen.setState(computeActiveState(screen));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshAfterBinding(String screenId) {
        DocScreen screen = docScreenRepository.findById(screenId).orElse(null);
        if (screen == null) {
            return;
        }
        applyLifecycle(screen, null);
        docScreenRepository.save(screen);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void touchLastSeen(String screenId) {
        if (screenId == null || screenId.isBlank()) {
            return;
        }
        docScreenRepository.findById(screenId).ifPresent(screen -> {
            screen.setLastSeenAt(new Date());
            docScreenRepository.save(screen);
        });
    }

    @Override
    public boolean isOnline(DocScreen screen) {
        if (screen == null || screen.getLastSeenAt() == null) {
            return false;
        }
        return System.currentTimeMillis() - screen.getLastSeenAt().getTime() <= ONLINE_WINDOW_MS;
    }

    @Override
    public void enrichOutput(ScreensOutput output, DocScreen screen) {
        if (output == null || screen == null) {
            return;
        }
        boolean configComplete = isConfigComplete(screen);
        output.setConfigComplete(configComplete);
        output.setOnline(isOnline(screen));
        output.setLastSeenAt(screen.getLastSeenAt());
        output.setStateModel(formatStateModel(screen.getState()));
    }

    private int computeActiveState(DocScreen screen) {
        return isConfigComplete(screen)
                ? TerminalConstants.SCREEN_STATE_ACTIVE
                : TerminalConstants.SCREEN_STATE_PENDING;
    }

    private static String formatStateModel(Integer state) {
        if (state != null && state == TerminalConstants.SCREEN_STATE_ACTIVE) {
            return "已启用";
        }
        if (state != null && state == TerminalConstants.SCREEN_STATE_PENDING) {
            return "待配置";
        }
        return "冻结";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
