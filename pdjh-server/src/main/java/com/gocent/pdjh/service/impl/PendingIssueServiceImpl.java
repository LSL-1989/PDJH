package com.gocent.pdjh.service.impl;

import com.gocent.pdjh.entity.PendingIssue;
import com.gocent.pdjh.repository.PendingIssueRepository;
import com.gocent.pdjh.service.PendingIssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PendingIssueServiceImpl implements PendingIssueService {

    private final PendingIssueRepository pendingIssueRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PendingIssue raise(String issueType, String title, String detail, String refId, String payloadJson) {
        if (refId != null && !refId.isBlank()) {
            var existing = pendingIssueRepository.findFirstByIssueTypeAndRefIdAndStatus(
                    issueType, refId, PendingIssue.STATUS_OPEN);
            if (existing.isPresent()) {
                PendingIssue p = existing.get();
                p.setTitle(title);
                p.setDetail(detail);
                p.setPayloadJson(payloadJson);
                return pendingIssueRepository.save(p);
            }
        }
        PendingIssue p = new PendingIssue();
        p.setIssueType(issueType);
        p.setTitle(title);
        p.setDetail(detail);
        p.setRefId(refId);
        p.setPayloadJson(payloadJson);
        p.setStatus(PendingIssue.STATUS_OPEN);
        p.setCreateDate(new Date());
        return pendingIssueRepository.save(p);
    }

    @Override
    public List<Map<String, Object>> listOpen() {
        return pendingIssueRepository.findByStatusOrderByCreateDateDesc(PendingIssue.STATUS_OPEN).stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    @Override
    public long countOpen() {
        return pendingIssueRepository.countByStatus(PendingIssue.STATUS_OPEN);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolve(Long id) {
        if (id == null) return;
        pendingIssueRepository.findById(id).ifPresent(p -> {
            p.setStatus(PendingIssue.STATUS_RESOLVED);
            p.setResolveDate(new Date());
            pendingIssueRepository.save(p);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveByTypeAndRef(String issueType, String refId) {
        if (issueType == null || refId == null) return;
        pendingIssueRepository.findFirstByIssueTypeAndRefIdAndStatus(issueType, refId, PendingIssue.STATUS_OPEN)
                .ifPresent(p -> {
                    p.setStatus(PendingIssue.STATUS_RESOLVED);
                    p.setResolveDate(new Date());
                    pendingIssueRepository.save(p);
                });
    }

    private Map<String, Object> toMap(PendingIssue p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("issueType", p.getIssueType());
        m.put("title", p.getTitle());
        m.put("detail", p.getDetail());
        m.put("refId", p.getRefId());
        m.put("payloadJson", p.getPayloadJson());
        m.put("createDate", p.getCreateDate());
        m.put("route", routeFor(p.getIssueType()));
        return m;
    }

    private static String routeFor(String type) {
        if (PendingIssue.TYPE_DEPT_ID_CONFLICT.equals(type) || PendingIssue.TYPE_DEPT_NAME_CONFLICT.equals(type)) {
            return "/hospitalInfo/DeptManages";
        }
        if (PendingIssue.TYPE_DOCTOR_NO_DEPT.equals(type) || PendingIssue.TYPE_PASSWORD_RESET.equals(type)) {
            return "/hospitalInfo/DoctorProfile";
        }
        return "/dashboard";
    }
}
