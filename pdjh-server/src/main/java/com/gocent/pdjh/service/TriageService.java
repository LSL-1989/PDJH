package com.gocent.pdjh.service;

import com.gocent.pdjh.dto.TerminalSignInResult;

public interface TriageService {
    boolean checkIn(Integer callListId);
    boolean checkIn(Integer callListId, String deviceCode);

    /**
     * 自助签到终端核销。失败返回 null。
     * 与 boolean checkIn 的区别：带回队列人数、排队位置，并写首次/重复签到日志。
     */
    TerminalSignInResult checkInForTerminal(Integer callListId, String deviceCode);
    boolean markNoShow(Integer callListId);
    boolean suspendForTest(Integer callListId);
    boolean returnFromTest(Integer callListId);
}
