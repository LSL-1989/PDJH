package com.gocent.pdjh.service.support;

import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.enums.PatientFlowEvent;
import com.gocent.pdjh.repository.CallListRepository;
import com.gocent.pdjh.service.PatientFlowLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自助签到的队列位置与流程日志文案。
 * 消费者：门诊 Triage/CheckIn、药房签到、检验签到。
 * 位置按签到时间先后，不用预约优先的叫号序。
 */
@Component
@RequiredArgsConstructor
public class SignInQueueSupport {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Comparator<CallList> CHECK_IN_ORDER = Comparator
            .comparing(CallList::getCheckInTime, Comparator.nullsLast(Date::compareTo))
            .thenComparing(c -> c.getXh() == null ? Integer.MAX_VALUE : c.getXh())
            .thenComparing(c -> c.getId() == null ? Integer.MAX_VALUE : c.getId());

    private final CallListRepository callListRepository;
    private final PatientFlowLogService patientFlowLogService;

    /** 当日同诊室仍在等候的人数，以及本人在已签到顺序中的位置 */
    public SignInQueueFacts locate(CallList self) {
        if (self == null || self.getOfficeRoomId() == null || self.getOfficeRoomId().isBlank()) {
            return SignInQueueFacts.empty();
        }
        List<CallList> queue = callListRepository
                .findTodayCallListByOfficeRoomAndState(self.getOfficeRoomId(), 0)
                .stream()
                .filter(this::inWaitingPool)
                .sorted(CHECK_IN_ORDER)
                .toList();
        int position = 0;
        for (int i = 0; i < queue.size(); i++) {
            if (self.getId() != null && self.getId().equals(queue.get(i).getId())) {
                position = i + 1;
                break;
            }
        }
        return new SignInQueueFacts(queue.size(), position);
    }

    public String formatClock(Date date) {
        if (date == null) {
            return "";
        }
        return CLOCK.format(date.toInstant().atZone(ZoneId.systemDefault()));
    }

    /** 流程日志备注：序号、队列人数、排队位置、首次或重复 */
    public String flowRemark(boolean alreadyCheckedIn, String showNo, SignInQueueFacts facts) {
        String no = showNo == null || showNo.isBlank() ? "—" : showNo.trim();
        String pos = facts.queuePosition() > 0 ? ("第" + facts.queuePosition() + "位") : "—";
        String kind = alreadyCheckedIn ? "重复签到" : "首次签到";
        return "序号" + no + "，当前队列" + facts.queueCount() + "人，排队位置" + pos + "，" + kind;
    }

    /** 终端签到留痕。首次与重复都写，备注格式相同 */
    public void recordTerminal(CallList call, PatientFlowEvent event, String deviceCode, String operatorName,
                               boolean alreadyCheckedIn, SignInQueueFacts facts) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("deviceCode", deviceCode);
        payload.put("alreadyCheckedIn", alreadyCheckedIn);
        payload.put("queueCount", facts.queueCount());
        payload.put("queuePosition", facts.queuePosition());
        payload.put("showNo", call != null ? call.getShowNo() : null);
        patientFlowLogService.record(call, event, "TERMINAL", deviceCode, operatorName,
                flowRemark(alreadyCheckedIn, call != null ? call.getShowNo() : null, facts), payload);
    }

    private boolean inWaitingPool(CallList call) {
        String triage = call.getTriageState();
        if (triage == null || triage.isBlank()) {
            return true;
        }
        return "WAITING".equalsIgnoreCase(triage)
                || "REVISIT".equalsIgnoreCase(triage)
                || "PREPARING".equalsIgnoreCase(triage);
    }

    public record SignInQueueFacts(int queueCount, int queuePosition) {
        public static SignInQueueFacts empty() {
            return new SignInQueueFacts(0, 0);
        }
    }
}
