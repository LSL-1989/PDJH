package com.gocent.pdjh.service;

import com.gocent.pdjh.entity.GwAuditLog;
import com.gocent.pdjh.repository.GwAuditLogRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 集成网关调用审计：写入与查询 gw_audit_log。
 * 消费者：IntegrationEngineService 落库；AuditLogController 给管理后台查询和导出。
 */
@Service
public class AuditLogService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int EXPORT_LIMIT = 2000;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private GwAuditLogRepository auditLogRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SystemActionLogService systemActionLogService;

    /**
     * 异步保存审计日志，不在主线程中阻塞外部调用耗时。
     */
    public void logAsync(String endpointCode, String connectionId, String requestPayload, String responsePayload,
                         boolean isSuccess, long durationMs, String errorMessage) {
        CompletableFuture.runAsync(() -> {
            try {
                GwAuditLog log = new GwAuditLog();
                log.setEndpointCode(endpointCode);
                log.setConnectionId(connectionId);
                log.setRequestPayload(requestPayload != null && requestPayload.length() > 5000 ? requestPayload.substring(0, 5000) : requestPayload);
                log.setResponsePayload(responsePayload != null && responsePayload.length() > 5000 ? responsePayload.substring(0, 5000) : responsePayload);
                log.setIsSuccess(isSuccess);
                log.setDurationMs(durationMs);
                log.setErrorMessage(errorMessage != null && errorMessage.length() > 2000 ? errorMessage.substring(0, 2000) : errorMessage);
                log.setStatusCode(isSuccess ? 200 : 500);
                log.setCreateTime(new Date());

                auditLogRepository.save(log);
            } catch (Exception e) {
                System.err.println("Failed to async save audit log: " + e.getMessage());
            }
        });
    }

    /**
     * 按筛选条件分页查询。page 从 1 开始；stats 是筛选命中的全量，不是当前页。
     * 时间格式错误时抛 IllegalArgumentException。
     */
    public Map<String, Object> query(Map<String, Object> params) {
        Filter filter = parseFilter(params, true);
        Specification<GwAuditLog> spec = buildSpec(filter);
        Page<GwAuditLog> page = auditLogRepository.findAll(spec, PageRequest.of(
                filter.page - 1, filter.size, Sort.by(Sort.Direction.DESC, "createTime")));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", page.getContent());
        result.put("total", page.getTotalElements());
        result.put("stats", summarize(spec, page.getTotalElements(), filter.isSuccess));
        return result;
    }

    /**
     * 按与查询相同的条件导出 CSV，含请求和响应，最多 {@value #EXPORT_LIMIT} 条。
     * 导出动作写入 sys_action_log（DATA_EXPORT），不另建数据访问日志表。
     */
    public Map<String, Object> exportCsv(Map<String, Object> params, String userNo, String ip) {
        Filter filter = parseFilter(params, false);
        Specification<GwAuditLog> spec = buildSpec(filter);
        Page<GwAuditLog> page = auditLogRepository.findAll(spec, PageRequest.of(
                0, EXPORT_LIMIT, Sort.by(Sort.Direction.DESC, "createTime")));
        boolean truncated = page.getTotalElements() > page.getNumberOfElements();
        String csv = toCsv(page.getContent());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("endpointCode", filter.endpointCode);
        payload.put("isSuccess", filter.isSuccess);
        payload.put("startTime", formatTime(filter.startTime));
        payload.put("endTime", formatTime(filter.endTime));
        payload.put("minDurationMs", filter.minDurationMs);
        payload.put("keyword", filter.keyword);
        payload.put("matched", page.getTotalElements());
        payload.put("exported", page.getNumberOfElements());
        payload.put("truncated", truncated);
        payload.put("ip", ip);
        String operator = userNo == null || userNo.isBlank() ? "SYS" : userNo.trim();
        if (operator.length() > 100) {
            operator = operator.substring(0, 100);
        }
        String desc = "导出外部网关审计日志 " + page.getNumberOfElements() + " 条"
                + (truncated ? "（命中 " + page.getTotalElements() + " 条，已截断）" : "");
        systemActionLogService.recordLogAsync("DATA_EXPORT", operator, desc, payload, true, null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("csv", csv);
        result.put("count", page.getNumberOfElements());
        result.put("total", page.getTotalElements());
        result.put("truncated", truncated);
        return result;
    }

    private Map<String, Object> summarize(Specification<GwAuditLog> spec, long total, Boolean isSuccess) {
        long success;
        long fail;
        if (Boolean.TRUE.equals(isSuccess)) {
            success = total;
            fail = 0;
        } else if (Boolean.FALSE.equals(isSuccess)) {
            success = 0;
            fail = total;
        } else {
            success = auditLogRepository.count(spec.and((root, query, cb) -> cb.isTrue(root.get("isSuccess"))));
            fail = total - success;
        }
        long avg = 0;
        if (total > 0) {
            var cb = entityManager.getCriteriaBuilder();
            var cq = cb.createQuery(Double.class);
            var root = cq.from(GwAuditLog.class);
            cq.select(cb.avg(root.get("durationMs")));
            Predicate predicate = spec.toPredicate(root, cq, cb);
            if (predicate != null) {
                cq.where(predicate);
            }
            Double value = entityManager.createQuery(cq).getSingleResult();
            avg = value == null ? 0 : Math.round(value);
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("success", success);
        stats.put("fail", fail);
        stats.put("avgDuration", avg);
        return stats;
    }

    private Specification<GwAuditLog> buildSpec(Filter filter) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (filter.endpointCode != null) {
                preds.add(cb.like(root.get("endpointCode"), "%" + filter.endpointCode + "%"));
            }
            if (filter.isSuccess != null) {
                preds.add(cb.equal(root.get("isSuccess"), filter.isSuccess));
            }
            if (filter.startTime != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("createTime"), filter.startTime));
            }
            if (filter.endTime != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("createTime"), filter.endTime));
            }
            if (filter.minDurationMs > 0) {
                preds.add(cb.greaterThanOrEqualTo(root.get("durationMs"), filter.minDurationMs));
            }
            if (filter.keyword != null) {
                String like = "%" + filter.keyword + "%";
                preds.add(cb.or(
                        cb.like(root.get("requestPayload"), like),
                        cb.like(root.get("responsePayload"), like),
                        cb.like(root.get("errorMessage"), like)));
            }
            return preds.isEmpty() ? cb.conjunction() : cb.and(preds.toArray(new Predicate[0]));
        };
    }

    private Filter parseFilter(Map<String, Object> params, boolean paged) {
        Map<String, Object> body = params == null ? Map.of() : params;
        Filter filter = new Filter();
        filter.endpointCode = blankToNull(body.get("endpointCode"));
        filter.isSuccess = parseBoolean(body.get("isSuccess"));
        filter.startTime = parseTime(body.get("startTime"), "开始时间");
        filter.endTime = parseTime(body.get("endTime"), "结束时间");
        if (filter.startTime != null && filter.endTime != null && filter.startTime.after(filter.endTime)) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }
        filter.minDurationMs = parseLong(body.get("minDurationMs"));
        if (filter.minDurationMs < 0) {
            filter.minDurationMs = 0;
        }
        filter.keyword = blankToNull(body.get("keyword"));
        if (filter.keyword != null && filter.keyword.length() > 100) {
            filter.keyword = filter.keyword.substring(0, 100);
        }
        if (paged) {
            int page = (int) parseLong(body.get("page"));
            int size = (int) parseLong(body.get("size"));
            filter.page = page < 1 ? 1 : page;
            filter.size = size < 1 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        }
        return filter;
    }

    private static Boolean parseBoolean(Object raw) {
        if (raw instanceof Boolean value) {
            return value;
        }
        if (raw == null) {
            return null;
        }
        String text = raw.toString().trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        throw new IllegalArgumentException("状态参数不正确");
    }

    private static Date parseTime(Object raw, String label) {
        if (raw == null) {
            return null;
        }
        String text = raw.toString().trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            LocalDateTime time = LocalDateTime.parse(text, TIME_FORMAT);
            return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(label + "格式应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private static long parseLong(Object raw) {
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw == null) {
            return 0;
        }
        String text = raw.toString().trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return 0;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("数字参数不正确");
        }
    }

    private static String blankToNull(Object raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.toString().trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
    }

    private static String toCsv(List<GwAuditLog> rows) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("ID,接口代码,连接ID,状态,状态码,耗时ms,错误信息,调用时间,请求Payload,响应Payload\n");
        for (GwAuditLog row : rows) {
            csv.append(csvCell(row.getId())).append(',')
                    .append(csvCell(row.getEndpointCode())).append(',')
                    .append(csvCell(row.getConnectionId())).append(',')
                    .append(csvCell(Boolean.TRUE.equals(row.getIsSuccess()) ? "成功" : "失败")).append(',')
                    .append(csvCell(row.getStatusCode())).append(',')
                    .append(csvCell(row.getDurationMs())).append(',')
                    .append(csvCell(row.getErrorMessage())).append(',')
                    .append(csvCell(formatTime(row.getCreateTime()))).append(',')
                    .append(csvCell(row.getRequestPayload())).append(',')
                    .append(csvCell(row.getResponsePayload()))
                    .append('\n');
        }
        return csv.toString();
    }

    private static String formatTime(Date time) {
        if (time == null) {
            return "";
        }
        return TIME_FORMAT.format(time.toInstant().atZone(ZoneId.systemDefault()));
    }

    private static String csvCell(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString().replace("\"", "\"\"");
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text + "\"";
        }
        return text;
    }

    private static final class Filter {
        private String endpointCode;
        private Boolean isSuccess;
        private Date startTime;
        private Date endTime;
        private long minDurationMs;
        private String keyword;
        private int page = 1;
        private int size = 20;
    }
}
