package com.gocent.pdjh.service;

import java.util.List;
import java.util.Map;

public interface CallListScreenService {
    List<Map<String, Object>> getCallListInput(String screenId);

    /**
     * 按屏幕绑定诊室分页取当日队列。
     * includePreparing 为 true 时，state=0 额外包含药房 PREPARING（配药中），供窗口屏；
     * 缺省 false，门诊等候列表行为不变。不走 /queue/pharmacyDeskQueue：那边按单个窗口 ID，且给叫号器。
     */
    Map<String, Object> getCallListInputPage(String screenId, String code, Integer state, Integer index, Integer page, boolean includePreparing);
}
