package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.exception.TerminalBindingException;
import com.gocent.pdjh.repository.CallListRepository;
import com.gocent.pdjh.repository.ScreenRoomMappingRepository;
import com.gocent.pdjh.service.CallListScreenService;
import com.gocent.pdjh.service.TerminalBindingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CallListScreenServiceImpl implements CallListScreenService {

    private final CallListRepository callListRepository;
    private final TerminalBindingService terminalBindingService;
    private final ScreenRoomMappingRepository screenRoomMappingRepository;

    @Override
    public List<Map<String, Object>> getCallListInput(String screenId) {
        List<String> roomIds = resolveAllowedRooms(screenId, null);
        List<CallList> attending = callListRepository.findTodayCallListByOfficeRoomsAndState(roomIds, 1);
        return attending.stream().map(this::toLegacyMap).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getCallListInputPage(String screenId, String code, Integer state, Integer index, Integer page, boolean includePreparing) {
        List<String> roomIds = resolveAllowedRooms(screenId, code);
        List<CallList> all = queryByState(roomIds, state != null ? state : 0, includePreparing);
        int pageSize = page != null && page > 0 ? page : 7;
        int pageIndex = index != null && index > 0 ? index : 1;
        int total = all.size();
        int pageCount = pageSize > 0 ? (int) Math.ceil(total / (double) pageSize) : 0;
        if (pageCount == 0 && total > 0) {
            pageCount = 1;
        }
        int from = Math.min((pageIndex - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<Map<String, Object>> data = all.subList(from, to).stream()
                .map(this::toLegacyMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("Data", data);
        result.put("Totle", total);
        result.put("Index", pageIndex);
        result.put("PageCount", pageCount);
        return result;
    }

    private List<String> resolveAllowedRooms(String screenId, String code) {
        List<String> roomIds = screenRoomMappingRepository.findByScreenId(screenId).stream()
                .map(m -> m.getOfficeRoomId())
                .collect(Collectors.toList());
        if (roomIds.isEmpty()) {
            throw new TerminalBindingException(403, "大屏尚未绑定物理诊室，请在管理后台完成诊室绑定");
        }
        if (code != null && !code.isBlank() && !roomIds.contains(code)) {
            throw new TerminalBindingException(403, "请求的诊室不在该大屏绑定范围内");
        }
        if (code != null && !code.isBlank()) {
            return List.of(code);
        }
        return roomIds;
    }

    private List<CallList> queryByState(List<String> roomIds, int state, boolean includePreparing) {
        List<CallList> result = new ArrayList<>();
        for (String roomId : roomIds) {
            if (state == 0) {
                result.addAll(callListRepository.findTodayCallListByOfficeRoomAndState(roomId, 0).stream()
                        .filter(c -> {
                            String triage = c.getTriageState();
                            if (triage == null || "WAITING".equals(triage) || "REVISIT".equals(triage)) {
                                return true;
                            }
                            return includePreparing && "PREPARING".equals(triage);
                        })
                        .collect(Collectors.toList()));
            } else if (state == 1) {
                result.addAll(callListRepository.findTodayCallListByOfficeRoomAndState(roomId, 1));
            } else if (state == 3) {
                result.addAll(callListRepository.findTodayCallListByOfficeRoomAndState(roomId, 3));
                result.addAll(callListRepository.findTodayCallListByOfficeRoomAndState(roomId, 0).stream()
                        .filter(c -> "PASSED".equals(c.getTriageState()))
                        .collect(Collectors.toList()));
            } else {
                result.addAll(callListRepository.findTodayCallListByOfficeRoomAndState(roomId, state));
            }
        }
        result.sort((a, b) -> {
            int ax = a.getXh() != null ? a.getXh() : 0;
            int bx = b.getXh() != null ? b.getXh() : 0;
            return Integer.compare(ax, bx);
        });
        return result;
    }

    private Map<String, Object> toLegacyMap(CallList c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("PatientName", c.getPatientName());
        m.put("PatientCode", c.getPatientCode());
        m.put("OutPatientOffice", c.getOutPatientOffice());
        m.put("DeptId", c.getDeptID());
        m.put("DeptID", c.getDeptID());
        m.put("state", c.getState());
        m.put("xh", c.getXh());
        m.put("xhlb", c.getXhlb());
        m.put("ShowNo", c.getShowNo());
        m.put("sourceType", c.getSourceType());
        m.put("OfficeRoomId", c.getOfficeRoomId());
        /** 初/复展示：候诊大屏 default-mul 等按 TriageState=REVISIT 显示「复」 */
        m.put("TriageState", c.getTriageState());
        return m;
    }
}
