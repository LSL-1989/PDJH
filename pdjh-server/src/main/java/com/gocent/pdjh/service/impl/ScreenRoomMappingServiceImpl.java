package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.dto.OfficeRoomBrief;
import com.gocent.pdjh.entity.ScreenRoomMapping;
import com.gocent.pdjh.repository.ScreenRoomMappingRepository;
import com.gocent.pdjh.service.ScreenLifecycleService;
import com.gocent.pdjh.service.ScreenRoomMappingService;
import com.gocent.pdjh.service.TerminalBindingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenRoomMappingServiceImpl implements ScreenRoomMappingService {

    private final ScreenRoomMappingRepository screenRoomMappingRepository;
    private final TerminalBindingService terminalBindingService;
    private final ScreenLifecycleService screenLifecycleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean relevancyScreenAndRooms(String screenId, List<String> officeRoomIds) {
        screenRoomMappingRepository.deleteByScreenId(screenId);
        if (officeRoomIds != null && !officeRoomIds.isEmpty()) {
            List<ScreenRoomMapping> mappings = officeRoomIds.stream().map(roomId -> {
                ScreenRoomMapping mapping = new ScreenRoomMapping();
                mapping.setScreenId(screenId);
                mapping.setOfficeRoomId(roomId);
                return mapping;
            }).collect(Collectors.toList());
            screenRoomMappingRepository.saveAll(mappings);
        }
        log.info("大屏与诊室关联更新成功，屏幕ID: {}", screenId);
        screenLifecycleService.refreshAfterBinding(screenId);
        return true;
    }

    @Override
    public List<OfficeRoomBrief> getAssociatedRooms(String screenId) {
        List<String> roomIds = screenRoomMappingRepository.findByScreenId(screenId).stream()
                .map(ScreenRoomMapping::getOfficeRoomId)
                .collect(Collectors.toList());
        return terminalBindingService.toRoomBriefs(roomIds);
    }

    @Override
    public String buildRoomInfoString(String screenId) {
        return getAssociatedRooms(screenId).stream()
                .map(r -> r.getId() + "|" + r.getName())
                .collect(Collectors.joining(";"));
    }
}
