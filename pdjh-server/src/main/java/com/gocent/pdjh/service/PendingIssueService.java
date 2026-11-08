package com.gocent.pdjh.service;

import com.gocent.pdjh.entity.PendingIssue;

import java.util.List;
import java.util.Map;

/** 后台首页待处理事项：登记冲突/未绑科室，供 Dashboard 聚合 */
public interface PendingIssueService {

    PendingIssue raise(String issueType, String title, String detail, String refId, String payloadJson);

    List<Map<String, Object>> listOpen();

    long countOpen();

    void resolve(Long id);

    void resolveByTypeAndRef(String issueType, String refId);
}
