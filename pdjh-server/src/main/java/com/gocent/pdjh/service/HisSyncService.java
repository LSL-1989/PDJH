package com.gocent.pdjh.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gocent.pdjh.entity.Department;
import com.gocent.pdjh.entity.Doctor;
import com.gocent.pdjh.entity.PendingIssue;
import com.gocent.pdjh.entity.SysConfig;
import com.gocent.pdjh.repository.DepartmentRepository;
import com.gocent.pdjh.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HisSyncService {

    public static final String CONFIG_KEY_DEPT = "HIS_SYNC_DEPT";
    public static final String CONFIG_KEY_DOCTOR = "HIS_SYNC_DOCTOR";

    private final SysConfigService sysConfigService;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final PendingIssueService pendingIssueService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> getSyncConfig(String configKey) {
        SysConfig cfg = sysConfigService.getConfigByKey(configKey);
        if (cfg == null || cfg.getConfigValue() == null || cfg.getConfigValue().isBlank()) {
            return defaultConfig();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(cfg.getConfigValue(), new TypeReference<Map<String, Object>>() {});
            defaultConfig().forEach(parsed::putIfAbsent);
            return parsed;
        } catch (Exception e) {
            return defaultConfig();
        }
    }

    @Transactional
    public Map<String, Object> saveAndSync(String configKey, Map<String, Object> body, boolean isDept) {
        String mode = str(body.get("mode"), "MANUAL");
        String url = str(body.get("url"), "");
        String cron = str(body.get("cron"), "MANUAL_ONLY");
        boolean triggerNow = Boolean.TRUE.equals(body.get("triggerNow"));

        Map<String, Object> config = loadOrDefault(configKey);
        config.put("mode", mode);
        config.put("url", url);
        config.put("cron", cron);

        if ("MANUAL".equalsIgnoreCase(mode)) {
            config.put("lastStatus", "SAVED");
            config.put("lastError", null);
            persistConfig(configKey, config);
            return Map.of("success", true, "synced", 0, "message", "已保存：手工维护模式");
        }

        if (url.isBlank()) {
            throw new IllegalArgumentException("请填写数据源地址或视图名称");
        }

        config.put("lastStatus", "SAVED");
        config.put("lastError", null);
        persistConfig(configKey, config);

        if (!triggerNow) {
            return Map.of("success", true, "synced", 0, "message", "调度配置已保存");
        }

        try {
            int count = isDept
                    ? syncDepartmentsFromSource(mode, url)
                    : syncDoctorsFromSource(mode, url);
            config.put("lastStatus", "SUCCESS");
            config.put("lastSyncTime", Instant.now().toString());
            config.put("lastSyncCount", count);
            config.put("lastError", null);
            persistConfig(configKey, config);
            return Map.of("success", true, "synced", count, "message", "同步完成，更新 " + count + " 条");
        } catch (Exception e) {
            log.warn("HIS 同步失败 [{}]: {}", configKey, e.getMessage());
            config.put("lastStatus", "FAILED");
            config.put("lastSyncTime", Instant.now().toString());
            config.put("lastSyncCount", 0);
            config.put("lastError", e.getMessage());
            persistConfig(configKey, config);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /** 仅探测连通性，不写业务库 */
    public Map<String, Object> probe(String configKey, Map<String, Object> body, boolean isDept) {
        String mode = str(body.get("mode"), "MANUAL");
        String url = str(body.get("url"), "");
        if ("MANUAL".equalsIgnoreCase(mode)) {
            return Map.of("reachable", true, "rowCountSample", 0, "message", "手工维护模式无需拨测");
        }
        if (url.isBlank()) {
            return Map.of("reachable", false, "rowCountSample", 0, "message", "请填写数据源地址或视图名称");
        }
        try {
            List<Map<String, Object>> rows = fetchRows(mode, url);
            String unit = isDept ? "条科室" : "名医生";
            return Map.of(
                    "reachable", true,
                    "rowCountSample", rows.size(),
                    "message", "通道正常，可读 " + rows.size() + unit + " 样本数据"
            );
        } catch (Exception e) {
            return Map.of(
                    "reachable", false,
                    "rowCountSample", 0,
                    "message", e.getMessage()
            );
        }
    }

    private Map<String, Object> loadOrDefault(String configKey) {
        Map<String, Object> cfg = new LinkedHashMap<>(defaultConfig());
        SysConfig sc = sysConfigService.getConfigByKey(configKey);
        if (sc != null && sc.getConfigValue() != null && !sc.getConfigValue().isBlank()) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(sc.getConfigValue(), new TypeReference<Map<String, Object>>() {});
                cfg.putAll(parsed);
            } catch (Exception ignored) {
            }
        }
        return cfg;
    }

    private int syncDepartmentsFromSource(String mode, String url) {
        List<Map<String, Object>> rows = fetchRows(mode, url);
        if (rows.isEmpty()) {
            throw new IllegalStateException("未拉取到任何科室数据，请检查数据源配置");
        }
        int count = 0;
        int conflicts = 0;
        for (Map<String, Object> row : rows) {
            String code = getRowStr(row, "deptCode", "code");
            String name = getRowStr(row, "deptName", "name");
            String hisId = getRowStr(row, "deptId", "id");
            if (code == null || name == null) continue;

            // 优先按 HIS 科室 ID 匹配；其次 queue/code；再名称
            Department byId = hisId != null ? departmentRepository.findById(hisId).orElse(null) : null;
            Department byCode = departmentRepository.findAll().stream()
                    .filter(d -> code.equalsIgnoreCase(d.getQueue()))
                    .findFirst().orElse(null);
            List<Department> byName = departmentRepository.findByName(name);

            if (byId != null) {
                // ID 已存在：名称不同则记冲突，不覆盖
                if (!name.equals(byId.getName())) {
                    pendingIssueService.raise(PendingIssue.TYPE_DEPT_NAME_CONFLICT,
                            "HIS 同步名称冲突：" + name,
                            "本地 ID=" + byId.getId() + " 现有名称=" + byId.getName() + "，HIS 名称=" + name,
                            byId.getId(), null);
                    conflicts++;
                    continue;
                }
                byId.setQueue(code);
                byId.setBusinessType(getRowStr(row, "businessType") != null ? getRowStr(row, "businessType") : "OUTPATIENT");
                String subType = getRowStr(row, "medTechSubType");
                if (subType != null) byId.setMedTechSubType(subType);
                departmentRepository.save(byId);
                count++;
                continue;
            }

            if (byCode != null && !byCode.getName().equals(name)) {
                pendingIssueService.raise(PendingIssue.TYPE_DEPT_NAME_CONFLICT,
                        "HIS 同步编码冲突：" + code,
                        "本地 queue 对应名称=" + byCode.getName() + "，HIS 名称=" + name,
                        byCode.getId(), null);
                conflicts++;
                continue;
            }

            if (!byName.isEmpty() && byCode == null) {
                Department exist = byName.get(0);
                pendingIssueService.raise(PendingIssue.TYPE_DEPT_NAME_CONFLICT,
                        "HIS 同步同名冲突：" + name,
                        "本地已有同名科室 ID=" + exist.getId() + "，HIS code=" + code
                                + (hisId != null ? " hisId=" + hisId : ""),
                        exist.getId(), null);
                conflicts++;
                continue;
            }

            Department dept = byCode != null ? byCode : new Department();
            if (dept.getId() == null) {
                dept.setId(hisId != null && !hisId.isBlank() ? hisId : UUID.randomUUID().toString());
                if (departmentRepository.existsById(dept.getId())) {
                    pendingIssueService.raise(PendingIssue.TYPE_DEPT_ID_CONFLICT,
                            "HIS 同步 ID 冲突：" + dept.getId(),
                            "HIS 名称=" + name + " code=" + code,
                            dept.getId(), null);
                    conflicts++;
                    continue;
                }
                dept.setFatherID("00000000-0000-0000-0000-000000000000");
                dept.setState(1);
                dept.setType(2); // 叶子科室
            }
            dept.setName(name);
            dept.setQueue(code);
            dept.setBusinessType(getRowStr(row, "businessType") != null ? getRowStr(row, "businessType") : "OUTPATIENT");
            String subType = getRowStr(row, "medTechSubType");
            if (subType != null) {
                dept.setMedTechSubType(subType);
            }
            departmentRepository.save(dept);
            count++;
        }
        if (conflicts > 0) {
            log.warn("HIS 科室同步完成：成功 {}，冲突已记入待处理 {}", count, conflicts);
        }
        return count;
    }

    private int syncDoctorsFromSource(String mode, String url) {
        List<Map<String, Object>> rows = fetchRows(mode, url);
        if (rows.isEmpty()) {
            throw new IllegalStateException("未拉取到任何医生数据，请检查数据源配置");
        }
        int count = 0;
        for (Map<String, Object> row : rows) {
            String workNo = getRowStr(row, "workNo", "doctorCode");
            String name = getRowStr(row, "doctorName", "name");
            if (workNo == null || name == null) continue;

            Doctor doc = doctorRepository.findAll().stream()
                    .filter(d -> workNo.equals(d.getWorkNo()))
                    .findFirst()
                    .orElse(null);
            if (doc == null) {
                doc = new Doctor();
                doc.setId(UUID.randomUUID().toString().replace("-", ""));
                doc.setCreateDate(new Date());
                doc.setIsOnDuty(1);
            }
            doc.setWorkNo(workNo);
            doc.setName(name);
            String title = getRowStr(row, "title");
            if (title != null) doc.setTitle(title);
            String deptId = getRowStr(row, "deptId");
            if (deptId != null) doc.setDeptId(deptId);
            String gender = getRowStr(row, "gender");
            if (gender != null) doc.setGender(gender);
            String specialty = getRowStr(row, "specialty");
            if (specialty != null) doc.setSpecialty(specialty);
            if (doc.getDisplayStatus() == null) {
                doc.setDisplayStatus(1);
            }
            doctorRepository.save(doc);
            count++;
        }
        return count;
    }

    private List<Map<String, Object>> fetchRows(String mode, String url) {
        if ("API".equalsIgnoreCase(mode)) {
            return fetchRowsFromHttp(url);
        }
        if ("VIEW".equalsIgnoreCase(mode)) {
            return fetchRowsFromView(url);
        }
        throw new IllegalArgumentException("不支持的数据交互模式: " + mode);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchRowsFromHttp(String url) {
        if (url == null || !url.startsWith("http")) {
            throw new IllegalArgumentException("API 模式请填写以 http 或 https 开头的完整接口地址");
        }
        try {
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null) {
                throw new IllegalStateException("接口返回为空");
            }
            Object data = resp.get("data");
            if (data instanceof List<?> list) {
                List<Map<String, Object>> out = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        out.add((Map<String, Object>) m);
                    }
                }
                return out;
            }
            throw new IllegalStateException("接口响应中未找到 data 数组");
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("HTTP 拉取失败: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> fetchRowsFromView(String viewName) {
        String safeName = validateSqlIdentifier(viewName);
        String sql = "SELECT * FROM `" + safeName + "`";
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            throw new IllegalStateException("数据库视图查询失败 [" + safeName + "]: " + e.getMessage(), e);
        }
    }

    private static String validateSqlIdentifier(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("视图或表名不能为空");
        }
        String trimmed = name.trim();
        if (!trimmed.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("视图或表名仅允许字母、数字和下划线");
        }
        return trimmed;
    }

    private void persistConfig(String key, Map<String, Object> config) {
        try {
            SysConfig sc = new SysConfig();
            sc.setConfigKey(key);
            sc.setConfigValue(objectMapper.writeValueAsString(config));
            sc.setDescription("HIS 同步调度配置");
            sysConfigService.saveOrUpdateConfig(sc);
        } catch (Exception e) {
            throw new RuntimeException("保存同步配置失败", e);
        }
    }

    private static Map<String, Object> defaultConfig() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mode", "MANUAL");
        m.put("url", "");
        m.put("cron", "MANUAL_ONLY");
        m.put("lastStatus", "NEVER");
        m.put("lastSyncTime", null);
        m.put("lastSyncCount", 0);
        m.put("lastError", null);
        return m;
    }

    private static String getRowStr(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            for (String variant : new String[]{key, key.toLowerCase(), key.toUpperCase()}) {
                String v = str(row.get(variant), null);
                if (v != null) return v;
            }
        }
        return null;
    }

    private static String str(Object v, String def) {
        if (v == null) return def;
        String s = v.toString().trim();
        return s.isEmpty() ? def : s;
    }
}
