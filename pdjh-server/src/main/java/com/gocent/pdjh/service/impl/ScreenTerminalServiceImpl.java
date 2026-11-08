package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.constant.TerminalConstants;
import com.gocent.pdjh.dto.ScreensOutput;
import com.gocent.pdjh.dto.terminal.TerminalRegisterOutput;
import com.gocent.pdjh.dto.terminal.TerminalResolveOutput;
import com.gocent.pdjh.entity.DocScreen;
import com.gocent.pdjh.repository.DocScreenRepository;
import com.gocent.pdjh.service.ScreenLifecycleService;
import com.gocent.pdjh.service.ScreenTerminalService;
import com.gocent.pdjh.service.ScreensService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenTerminalServiceImpl implements ScreenTerminalService {

    private final DocScreenRepository docScreenRepository;
    private final ScreensService screensService;
    private final ScreenLifecycleService screenLifecycleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TerminalRegisterOutput register(String deviceCode, String terminalType) {
        String code = deviceCode.trim();
        var existing = docScreenRepository.findByDeviceCode(code);
        if (existing.isPresent()) {
            DocScreen screen = existing.get();
            screenLifecycleService.touchLastSeen(screen.getId());
            TerminalRegisterOutput out = new TerminalRegisterOutput();
            out.setRecordId(screen.getId());
            out.setDeviceCode(screen.getDeviceCode());
            out.setTerminalType(screen.getTerminalType() != null ? screen.getTerminalType() : terminalType);
            out.setLifecycleState(screen.getState());
            out.setCreated(false);
            return out;
        }

        DocScreen screen = new DocScreen();
        screen.setId(UUID.randomUUID().toString());
        screen.setDeviceCode(code);
        screen.setTerminalType(terminalType);
        screen.setTemplateRoute(
                TerminalConstants.TYPE_CLINIC.equals(terminalType) ? null : TerminalConstants.DEFAULT_TEMPLATE_ROUTE);
        screen.setName("待配置-" + code.substring(0, Math.min(8, code.length())));
        screen.setState(TerminalConstants.SCREEN_STATE_PENDING);
        screen.setCreateDate(new Date());
        screen.setCallCode("CALL");
        screen.setCallNum(1);
        screen.setCallContent("请{0}{1}，到{2}检查");
        screen.setMaxNum(TerminalConstants.TYPE_CLINIC.equals(terminalType) ? 1 : 5);
        screen.setBulletin("请遵守次序，文明就诊。");
        screen.setScrollTime(5);
        screen.setSubtitle("候诊区");
        screen.setRate(0);
        screen.setIsNum("0");
        screen.setIsAudio("1");
        docScreenRepository.save(screen);
        screenLifecycleService.touchLastSeen(screen.getId());
        log.info("终端自助登记 doc_screen: {} type={}", code, terminalType);

        TerminalRegisterOutput out = new TerminalRegisterOutput();
        out.setRecordId(screen.getId());
        out.setDeviceCode(screen.getDeviceCode());
        out.setTerminalType(terminalType);
        out.setLifecycleState(screen.getState());
        out.setCreated(true);
        return out;
    }

    @Override
    public TerminalResolveOutput resolve(String deviceCode, String terminalType) {
        TerminalResolveOutput out = new TerminalResolveOutput();
        out.setDeviceCode(deviceCode);

        DocScreen screen = docScreenRepository.findByDeviceCode(deviceCode.trim()).orElse(null);
        if (screen == null) {
            out.setStatus(TerminalConstants.STATUS_NOT_REGISTERED);
            return out;
        }

        out.setRecordId(screen.getId());
        out.setScreenId(screen.getId());
        String actualType = screen.getTerminalType() != null && !screen.getTerminalType().isBlank()
                ? screen.getTerminalType().trim().toUpperCase()
                : TerminalConstants.TYPE_SCREEN;
        out.setTerminalType(actualType);
        screenLifecycleService.touchLastSeen(screen.getId());

        if (screen.getState() != null && screen.getState() == TerminalConstants.SCREEN_STATE_FROZEN) {
            out.setStatus(TerminalConstants.STATUS_FROZEN);
            return out;
        }

        // 优先按实际绑定关系判断；避免 state 仍为待配置但诊室已绑定时无法 READY
        if (screenLifecycleService.isBindingComplete(screen.getId(), terminalType)) {
            if (screen.getState() == null || screen.getState() == TerminalConstants.SCREEN_STATE_PENDING) {
                screenLifecycleService.refreshAfterBinding(screen.getId());
            }
            out.setStatus(TerminalConstants.STATUS_READY);
            if (TerminalConstants.TYPE_CLINIC.equals(terminalType)) {
                out.setTemplateRoute(null);
            } else {
                String route = screen.getTemplateRoute();
                out.setTemplateRoute(route != null && !route.isBlank() ? route : TerminalConstants.DEFAULT_TEMPLATE_ROUTE);
            }
            ScreensOutput full = screensService.getScreensSingle(screen.getId());
            if (full != null && full.getContentConfig() != null) {
                out.setContentConfig(full.getContentConfig());
            }
            return out;
        }

        out.setStatus(TerminalConstants.STATUS_PENDING);
        return out;
    }
}
