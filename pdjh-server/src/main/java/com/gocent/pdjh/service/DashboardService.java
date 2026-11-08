package com.gocent.pdjh.service;

import com.gocent.pdjh.entity.PendingIssue;
import com.gocent.pdjh.repository.DoctorRepository;
import com.gocent.pdjh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;
    private final PendingIssueService pendingIssueService;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    /** 后台首页待处理：冲突、未绑科室、密码重置申请 */
    public Map<String, Object> getPendingSummary() {
        // 同步扫描未绑科室医生（补全 pending，避免仅删科室时才写入）
        doctorRepository.findAll().stream()
                .filter(d -> d.getDeptId() == null || d.getDeptId().isBlank())
                .forEach(d -> pendingIssueService.raise(
                        PendingIssue.TYPE_DOCTOR_NO_DEPT,
                        "医生未绑定科室：" + (d.getName() != null ? d.getName() : d.getId()),
                        "请在医生档案中选择所属科室",
                        d.getId(), null));
        // 统一用户表的重置申请 → 用户管理
        userRepository.findByPasswordResetRequested(1).forEach(u -> pendingIssueService.raise(
                PendingIssue.TYPE_PASSWORD_RESET,
                "用户申请重置密码：" + (u.getUserName() != null ? u.getUserName() : u.getUserNo()),
                "请在用户管理中重置密码",
                u.getUserNo(), null));
        // 尚未迁入 users 的医生档案申请（兜底）
        doctorRepository.findAll().stream()
                .filter(d -> Integer.valueOf(1).equals(d.getPasswordResetRequested()))
                .forEach(d -> pendingIssueService.raise(
                        PendingIssue.TYPE_PASSWORD_RESET,
                        "医生申请重置密码：" + (d.getName() != null ? d.getName() : d.getWorkNo()),
                        "请在用户管理中为对应帐号重置密码（或先建统一用户）",
                        d.getId(), null));

        List<Map<String, Object>> items = pendingIssueService.listOpen();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("count", items.size());
        data.put("items", items);
        return data;
    }

    public void resolvePending(Long id) {
        pendingIssueService.resolve(id);
    }

    // 1. 获取当日核心业务 SLA
    public Map<String, Object> getOverviewMetrics() {
        String sql = """
            SELECT 
                COALESCE(AVG(TIMESTAMPDIFF(MINUTE, call_time, start_time)), 0) as avgWaitTime,
                COALESCE(AVG(TIMESTAMPDIFF(MINUTE, start_time, end_date)), 0) as avgProcessTime,
                SUM(CASE WHEN state = 0 THEN 1 ELSE 0 END) as waitingCount,
                SUM(CASE WHEN state = 4 THEN 1 ELSE 0 END) as completedCount,
                SUM(CASE WHEN state = 6 OR triage_state = 'PASSED' THEN 1 ELSE 0 END) as skipCount,
                COUNT(*) as totalCount
            FROM call_list 
            WHERE DATE(call_date) = CURRENT_DATE()
        """;
        log.info("【效能看板】计算当日核心 SLA 指标");
        return jdbcTemplate.queryForMap(sql);
    }

    // 2. 超时热告警
    public List<Map<String, Object>> getOvertimeAlerts(int thresholdMinutes) {
        String sql = """
            SELECT 
                c.patient_name as patientName,
                c.patient_code as patientCode,
                c.dept_id as deptId,
                COALESCE(d.name, c.dept_id) as deptName,
                COALESCE(doc.name, c.doctors, '') as doctor,
                COALESCE(c.doctor_id, '') as doctorId,
                TIMESTAMPDIFF(MINUTE, c.call_time, NOW()) as waitMinutes
            FROM call_list c
            LEFT JOIN department d ON d.id = c.dept_id
            LEFT JOIN doctor doc ON doc.id = c.doctor_id
            WHERE c.state = 0 
              AND DATE(c.call_date) = CURRENT_DATE()
              AND TIMESTAMPDIFF(MINUTE, c.call_time, NOW()) > ?
            ORDER BY waitMinutes DESC
        """;
        log.info("【效能看板】检索等待超过 {} 分钟的热点患者", thresholdMinutes);
        return jdbcTemplate.queryForList(sql, thresholdMinutes);
    }

    /**
     * 门诊主任台：医生出诊实时看板。
     * 含当前在诊（is_on_duty=1）及今日临时停诊（排班 OFF）的门诊医生，附排队/均等候。
     */
    public List<Map<String, Object>> getOutpatientDoctorDutyBoard() {
        String sql = """
            SELECT
              doc.id AS doctorId,
              doc.name AS doctorName,
              COALESCE(doc.title, '') AS title,
              COALESCE(doc.office_name, '') AS officeName,
              COALESCE(doc.is_on_duty, 0) AS isOnDuty,
              dept.id AS deptId,
              COALESCE(dept.name, '') AS deptName,
              COALESCE(q.waitingCount, 0) AS waitingCount,
              COALESCE(q.avgWaitMin, 0) AS avgWaitMin,
              CASE
                WHEN COALESCE(doc.is_on_duty, 0) = 1 THEN '在诊'
                WHEN EXISTS (
                  SELECT 1 FROM doctor_schedule s
                  WHERE s.doctor_id = doc.id AND s.schedule_date = CURRENT_DATE()
                    AND (s.shift_type = 'OFF' OR EXISTS (
                      SELECT 1 FROM dict_shift_type t
                      WHERE t.shift_code = s.shift_type AND COALESCE(t.exclusive_flag, 0) = 1
                    ))
                ) THEN '停诊'
                ELSE '离诊'
              END AS status
            FROM doctor doc
            INNER JOIN department dept ON dept.id = doc.dept_id AND dept.business_type = 'OUTPATIENT'
            LEFT JOIN (
              SELECT
                doctor_id,
                SUM(CASE WHEN state = 0 THEN 1 ELSE 0 END) AS waitingCount,
                COALESCE(AVG(CASE WHEN state = 0 THEN TIMESTAMPDIFF(MINUTE, call_time, NOW()) END), 0) AS avgWaitMin
              FROM call_list
              WHERE DATE(call_date) = CURRENT_DATE()
                AND doctor_id IS NOT NULL AND doctor_id <> ''
              GROUP BY doctor_id
            ) q ON q.doctor_id = doc.id
            WHERE COALESCE(doc.display_status, 1) = 1
              AND (
                COALESCE(doc.is_on_duty, 0) = 1
                OR EXISTS (
                  SELECT 1 FROM doctor_schedule s
                  WHERE s.doctor_id = doc.id AND s.schedule_date = CURRENT_DATE()
                    AND (s.shift_type = 'OFF' OR EXISTS (
                      SELECT 1 FROM dict_shift_type t
                      WHERE t.shift_code = s.shift_type AND COALESCE(t.exclusive_flag, 0) = 1
                    ))
                )
              )
            ORDER BY CASE WHEN COALESCE(doc.is_on_duty, 0) = 1 THEN 0 ELSE 1 END,
                     waitingCount DESC,
                     doc.name ASC
            """;
        log.info("【门诊主任台】查询医生出诊实时看板");
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 门诊主任台：科室过号率排行（门诊科室，按过号率降序）。
     */
    public List<Map<String, Object>> getOutpatientSkipRateRanking() {
        String sql = """
            SELECT
              dept.name AS name,
              COUNT(c.id) AS totalCount,
              SUM(CASE WHEN c.state = 6 OR c.triage_state = 'PASSED' THEN 1 ELSE 0 END) AS skipCount,
              COALESCE(
                SUM(CASE WHEN c.state = 6 OR c.triage_state = 'PASSED' THEN 1 ELSE 0 END) * 100.0
                  / NULLIF(COUNT(c.id), 0),
                0
              ) AS rate
            FROM department dept
            LEFT JOIN call_list c ON c.dept_id = dept.id AND DATE(c.call_date) = CURRENT_DATE()
            WHERE dept.business_type = 'OUTPATIENT'
            GROUP BY dept.id, dept.name
            HAVING totalCount > 0
            ORDER BY rate DESC, skipCount DESC
            LIMIT 10
            """;
        log.info("【门诊主任台】查询科室过号率排行");
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 门诊主任台：今日号源配额总览（排班 max_quota vs 当日已挂号）。
     */
    public List<Map<String, Object>> getOutpatientQuotaOverview() {
        String sql = """
            SELECT
              dept.name AS name,
              COALESCE(SUM(CASE WHEN NOT (s.shift_type = 'OFF' OR EXISTS (
                SELECT 1 FROM dict_shift_type t
                WHERE t.shift_code = s.shift_type AND COALESCE(t.exclusive_flag, 0) = 1
              )) THEN COALESCE(s.max_quota, 0) ELSE 0 END), 0) AS total,
              COALESCE(MAX(used.usedCount), 0) AS used
            FROM doctor_schedule s
            INNER JOIN department dept ON dept.id = s.dept_id AND dept.business_type = 'OUTPATIENT'
            LEFT JOIN (
              SELECT dept_id, COUNT(*) AS usedCount
              FROM call_list
              WHERE DATE(call_date) = CURRENT_DATE()
              GROUP BY dept_id
            ) used ON used.dept_id = s.dept_id
            WHERE s.schedule_date = CURRENT_DATE()
            GROUP BY dept.id, dept.name
            HAVING total > 0
            ORDER BY (used / NULLIF(total, 0)) DESC, name ASC
            """;
        log.info("【门诊主任台】查询今日号源配额总览");
        return jdbcTemplate.queryForList(sql);
    }

    // 3. 门诊峰值追踪（按小时分组统计入列量、接诊量、过号量）
    public List<Map<String, Object>> getPeakAnalysis() {
        return getPeakAnalysis(null);
    }

    public List<Map<String, Object>> getPeakAnalysis(String date) {
        String d = date == null || date.isBlank() ? LocalDate.now().toString() : date.substring(0, 10);
        String sql = """
            SELECT 
                HOUR(call_time) as hourOfDay,
                COUNT(*) as enqueueCount,
                SUM(CASE WHEN start_time IS NOT NULL THEN 1 ELSE 0 END) as processCount,
                SUM(CASE WHEN state = 6 OR triage_state = 'PASSED' THEN 1 ELSE 0 END) as skipCount
            FROM call_list
            WHERE DATE(call_date) = ?
            GROUP BY HOUR(call_time)
            ORDER BY hourOfDay ASC
        """;
        log.info("【效能看板】绘制分时接诊折线图数据 date={}", d);
        return jdbcTemplate.queryForList(sql, d);
    }

    // 4. 科室积压热力矩阵
    public List<Map<String, Object>> getHeatmapMatrix() {
        String sql = """
            SELECT 
                d.name as deptName,
                COUNT(c.id) as waitingCount,
                50 as capacityLimit
            FROM department d
            LEFT JOIN call_list c ON d.id = c.dept_id AND c.state = 0 AND DATE(c.call_date) = CURRENT_DATE()
            WHERE d.business_type IN ('OUTPATIENT', 'PHARMACY', 'OUTPATIENT_EXAM', 'DEFAULT_MEDTECH', 'HEALTH_CHECKUP')
            GROUP BY d.id, d.name
        """;
        log.info("【效能看板】抓取全院科室积压热力");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        
        for (Map<String, Object> row : rows) {
            int waiting = ((Number) row.get("waitingCount")).intValue();
            int capacity = ((Number) row.get("capacityLimit")).intValue();
            if (capacity <= 0) capacity = 50;
            
            double ratio = (double) waiting / capacity;
            String statusLevel = "IDLE";
            if (ratio >= 0.8) {
                statusLevel = "CONGESTED";
            } else if (ratio >= 0.4) {
                statusLevel = "BUSY";
            }
            row.put("statusLevel", statusLevel);
        }
        return rows;
    }

    /** 兼容旧版 dashboard store：各诊室等候人数卡片 */
    public List<Map<String, Object>> legacyDepartmentWaitCards() {
        String sql = """
            SELECT d.id AS deptId, d.name AS deptName,
                   COALESCE(SUM(CASE WHEN c.state = 0 THEN 1 ELSE 0 END), 0) AS waitCount
            FROM department d
            LEFT JOIN call_list c ON d.id = c.dept_id AND DATE(c.call_date) = CURDATE()
            GROUP BY d.id, d.name
            ORDER BY waitCount DESC
            LIMIT 80
            """;
        return jdbcTemplate.queryForList(sql);
    }

    /** 兼容旧版：按小时占位字段（前端折线组件） */
    public List<Map<String, Object>> legacyDepartmentQueueTime(Map<String, Object> input) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("Name", "全院");
        row.put("ZeroSpot", 0);
        row.put("TowSpot", 0);
        row.put("FourSpot", 0);
        row.put("SexSpot", 0);
        row.put("EightSpot", 0);
        row.put("TenSpot", 0);
        row.put("TwelveSpot", 0);
        row.put("FourteenSpot", 0);
        row.put("SixtenSpot", 0);
        row.put("EighttenSpot", 0);
        row.put("TwentySpot", 0);
        row.put("TwentyTwoSpot", 0);
        row.put("sflbbigtype", "ALL");
        List<Map<String, Object>> one = new ArrayList<>();
        one.add(row);
        return one;
    }

    /** 兼容旧版统计页：医生呼叫量分页 */
    public Map<String, Object> legacyDoctorCallCount(Map<String, Object> input) {
        int pageIndex = parseInt(input.get("PageIndex"), 1);
        int pageSize = parseInt(input.get("PageSize"), 10);
        String name = input.get("Name") == null ? "" : input.get("Name").toString().trim();
        String date = input.get("Date") == null || input.get("Date").toString().isEmpty()
                ? LocalDate.now().toString()
                : input.get("Date").toString().substring(0, 10);
        int offset = (Math.max(1, pageIndex) - 1) * Math.max(1, pageSize);

        String countSql = """
            SELECT COUNT(*) FROM (
              SELECT doctors FROM call_list
              WHERE DATE(call_date) = ? AND doctors IS NOT NULL AND doctors <> ''
                AND (? = '' OR doctors LIKE CONCAT('%', ?, '%'))
              GROUP BY doctors
            ) t
            """;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, date, name, name);
        if (total == null) {
            total = 0L;
        }

        String dataSql = """
            SELECT doctors AS UserNo, doctors AS Name, COUNT(*) AS Count
            FROM call_list
            WHERE DATE(call_date) = ? AND doctors IS NOT NULL AND doctors <> ''
              AND (? = '' OR doctors LIKE CONCAT('%', ?, '%'))
            GROUP BY doctors
            ORDER BY Count DESC
            LIMIT ? OFFSET ?
            """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(dataSql, date, name, name, pageSize, offset);

        return Map.of(
                "data", rows,
                "pageInfo", Map.of("totalCount", total)
        );
    }

    private static int parseInt(Object o, int def) {
        if (o == null) {
            return def;
        }
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public Map<String, Object> getStatisticsSummary(String date) {
        String d = date == null || date.isBlank() ? LocalDate.now().toString() : date.substring(0, 10);
        String sql = """
            SELECT
              COUNT(*) AS totalPatients,
              COALESCE(AVG(TIMESTAMPDIFF(MINUTE, call_time, start_time)), 0) AS avgWaitMin,
              COALESCE(SUM(CASE WHEN state = 6 OR triage_state = 'PASSED' THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(*), 0), 0) AS skipRate,
              COALESCE(SUM(CASE WHEN state = 4 THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(*), 0), 0) AS completionRate
            FROM call_list WHERE DATE(call_date) = ?
            """;
        return jdbcTemplate.queryForMap(sql, d);
    }

    public List<Map<String, Object>> getStatisticsHourly(String date) {
        String d = date == null || date.isBlank() ? LocalDate.now().toString() : date.substring(0, 10);
        return jdbcTemplate.queryForList("""
            SELECT HOUR(call_time) AS hourOfDay, COUNT(*) AS enqueueCount,
                   SUM(CASE WHEN state IN (4, 6) OR triage_state = 'PASSED' THEN 1 ELSE 0 END) AS doneCount
            FROM call_list WHERE DATE(call_date) = ? GROUP BY HOUR(call_time) ORDER BY hourOfDay
            """, d);
    }

    public List<Map<String, Object>> getStatisticsDeptRadar(String date) {
        String d = date == null || date.isBlank() ? LocalDate.now().toString() : date.substring(0, 10);
        return jdbcTemplate.queryForList("""
            SELECT d.name AS deptName,
                   COUNT(c.id) AS callCount,
                   COALESCE(AVG(TIMESTAMPDIFF(MINUTE, c.call_time, c.start_time)), 0) AS avgWaitMin,
                   COALESCE(SUM(CASE WHEN c.state = 6 OR c.triage_state = 'PASSED' THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(c.id), 0), 0) AS skipRate
            FROM department d
            LEFT JOIN call_list c ON d.id = c.dept_id AND DATE(c.call_date) = ?
            GROUP BY d.id, d.name HAVING callCount > 0 ORDER BY callCount DESC LIMIT 8
            """, d);
    }
}
