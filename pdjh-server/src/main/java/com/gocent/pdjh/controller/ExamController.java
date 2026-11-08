package com.gocent.pdjh.controller;

import com.gocent.pdjh.dto.ExamDashboardOutput;
import com.gocent.pdjh.entity.DictExamItem;
import com.gocent.pdjh.entity.ExamPackageItem;
import com.gocent.pdjh.entity.ExamRecordDetail;
import com.gocent.pdjh.repository.DictExamItemRepository;
import com.gocent.pdjh.repository.ExamPackageItemRepository;
import com.gocent.pdjh.repository.ExamRecordDetailRepository;
import com.gocent.pdjh.entity.CallList;
import com.gocent.pdjh.repository.CallListRepository;
import com.gocent.pdjh.entity.Department;
import com.gocent.pdjh.repository.DepartmentRepository;
import com.gocent.pdjh.service.ExamRoutingService;
import com.gocent.pdjh.service.WaitTimeEstimatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/exam")
@RequiredArgsConstructor
@Slf4j
public class ExamController {

    private final ExamRecordDetailRepository recordDetailRepository;
    private final DictExamItemRepository dictExamItemRepository;
    private final CallListRepository callListRepository;
    private final com.gocent.pdjh.service.ExamRoutingService examRoutingService;
    private final ExamPackageItemRepository examPackageItemRepository;
    private final com.gocent.pdjh.repository.ExamPackageRepository examPackageRepository;

    @Autowired
    private WaitTimeEstimatorService waitTimeEstimatorService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @PostMapping("/getPatientTodoList")
    public ExamDashboardOutput getPatientTodoList(@RequestBody Map<String, String> payload) {
        String patientCode = payload.get("patientCode");
        ExamDashboardOutput output = new ExamDashboardOutput();
        List<ExamDashboardOutput.ExamItemDto> pending = new ArrayList<>();
        List<ExamDashboardOutput.ExamItemDto> completed = new ArrayList<>();
        
        List<ExamRecordDetail> records = recordDetailRepository.findByPatientCode(patientCode);
        if (records == null || records.isEmpty()) return output;

        // 收集所有涉及的项目ID进行批量字典查询
        List<Integer> itemIds = records.stream()
                .map(ExamRecordDetail::getItemId)
                .collect(java.util.stream.Collectors.toList());
        List<DictExamItem> dictList = dictExamItemRepository.findAllById(itemIds);
        Map<Integer, DictExamItem> dictMap = dictList.stream()
                .collect(java.util.stream.Collectors.toMap(DictExamItem::getId, d -> d));

        // 批量查询关联科室的名称
        List<String> deptIds = dictList.stream()
                .map(DictExamItem::getAssociatedDeptId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, String> deptNameMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            List<Department> deptList = departmentRepository.findAllById(deptIds);
            deptNameMap = deptList.stream()
                    .collect(Collectors.toMap(Department::getId, Department::getName));
        }

        for (ExamRecordDetail detail : records) {
            ExamDashboardOutput.ExamItemDto dto = new ExamDashboardOutput.ExamItemDto();
            dto.setId(detail.getId());
            dto.setItemId(detail.getItemId());
            
            DictExamItem dict = dictMap.get(detail.getItemId());
            if (dict != null) {
                dto.setItemName(dict.getItemName());
                dto.setIsFasting(dict.getIsFasting());
                // 关联 department 表查询真正的科室名称
                String deptName = deptNameMap.get(dict.getAssociatedDeptId());
                dto.setSuggestedDeptName(deptName != null ? deptName : dict.getAssociatedDeptId());
            }

            int itemState = detail.getState() == null ? -1 : detail.getState();
            if (itemState == 0) {
                pending.add(dto);
            } else if (itemState == 1 || itemState == 2) {
                // 如果是正在排队/检查中，查询这个科室前面有多少人
                if (dict != null && dict.getAssociatedDeptId() != null) {
                   List<CallList> waitings = callListRepository.findTodayCallListByDeptAndState(dict.getAssociatedDeptId(), 0);
                   int waitCount = waitings == null ? 0 : waitings.size();
                   dto.setWaitingCount(waitCount);
                }
                output.setCurrent(dto);
            } else if (itemState == 3) {
                completed.add(dto);
            }
        }
        
        output.setPending(pending);
        output.setCompleted(completed);
        return output;
    }
    
    @PostMapping("/adjustQueue")
    public boolean adjustQueue(@RequestBody Map<String, String> payload) {
        String patientCode = payload.get("patientCode");
        log.info("【智能导检】收到患者主动提出调整算路请求: {}", patientCode);
        if (patientCode != null && !patientCode.isEmpty()) {
            examRoutingService.recalculateRoute(patientCode);
            return true;
        }
        return false;
    }

    @PostMapping("/suspendDept")
    public boolean suspendDept(@RequestBody Map<String, String> payload) {
        String deptId = payload.get("deptId");
        if (deptId != null && !deptId.isEmpty()) {
            examRoutingService.suspendDept(deptId);
            return true;
        }
        return false;
    }

    @PostMapping("/forceRoute")
    public boolean forceRoute(@RequestBody Map<String, Object> payload) {
        String patientCode = (String) payload.get("patientCode");
        Integer targetItemId = payload.get("targetItemId") != null ? Integer.valueOf(payload.get("targetItemId").toString()) : null;
        Boolean isPriority = payload.get("isPriority") != null ? Boolean.valueOf(payload.get("isPriority").toString()) : false;
        
        if (patientCode != null && targetItemId != null) {
            examRoutingService.forceRoute(patientCode, targetItemId, isPriority);
            return true;
        }
        return false;
    }

    /**
     * P1-4a: GET /exam/inProgress
     * 查询今日在途体检患者状态看板
     * 返回: [{patientCode, patientName, completedCount, totalCount, progressPct,
     *         currentDept, currentWaitCount, estimatedFinishMin, status}]
     */
    @org.springframework.web.bind.annotation.GetMapping("/inProgress")
    public List<Map<String, Object>> getInProgress() {
        // 1. 找出今日有体检记录且未完全完成的患者
        List<ExamRecordDetail> allRecords = recordDetailRepository.findAll().stream()
            .filter(r -> {
                if (r.getEnqueueTime() == null) return false;
                java.util.Calendar cal = java.util.Calendar.getInstance();
                java.util.Calendar todayCal = java.util.Calendar.getInstance();
                cal.setTime(r.getEnqueueTime());
                return cal.get(java.util.Calendar.DAY_OF_YEAR) == todayCal.get(java.util.Calendar.DAY_OF_YEAR)
                    && cal.get(java.util.Calendar.YEAR) == todayCal.get(java.util.Calendar.YEAR);
            }).toList();

        // 2. 按患者分组
        Map<String, List<ExamRecordDetail>> byPatient = allRecords.stream()
            .collect(java.util.stream.Collectors.groupingBy(ExamRecordDetail::getPatientCode));

        // 3. 预加载字典（itemId -> avgTimeMinutes）
        List<Integer> allItemIds = allRecords.stream()
            .map(ExamRecordDetail::getItemId).distinct().collect(java.util.stream.Collectors.toList());
        Map<Integer, DictExamItem> dictMap = dictExamItemRepository.findAllById(allItemIds).stream()
            .collect(java.util.stream.Collectors.toMap(DictExamItem::getId, d -> d));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<ExamRecordDetail>> entry : byPatient.entrySet()) {
            String code = entry.getKey();
            List<ExamRecordDetail> records = entry.getValue();

            long total     = records.size();
            long completed = records.stream().filter(r -> Integer.valueOf(3).equals(r.getState())).count();
            long pending   = total - completed;
            if (total == 0 || completed == total) continue; // 跳过全完成的

            // 预估剩余时间（剩余项 × avgTimeMinutes + 队列等待估算30秒/人）
            int estimatedMin = records.stream()
                .filter(r -> !Integer.valueOf(3).equals(r.getState()))
                .mapToInt(r -> {
                    DictExamItem dict = dictMap.get(r.getItemId());
                    return dict != null && dict.getAvgTimeMinutes() != null ? dict.getAvgTimeMinutes() : 10;
                }).sum();

            // 当前科室（找最近一个待完成记录的科室）
            ExamRecordDetail currentRecord = records.stream()
                .filter(r -> !Integer.valueOf(3).equals(r.getState()))
                .findFirst().orElse(null);
            String currentDept = "";
            int waitCount = 0;
            Department cDeptEntity = null;
            if (currentRecord != null) {
                DictExamItem dict = dictMap.get(currentRecord.getItemId());
                if (dict != null && dict.getAssociatedDeptId() != null) {
                    currentDept = dict.getAssociatedDeptId();
                    cDeptEntity = departmentRepository.findById(currentDept).orElse(null);
                    // 查该科室等候人数
                    List<CallList> deptWaiting = callListRepository.findTodayCallListByDeptAndState(currentDept, 0);
                    waitCount = deptWaiting.size();
                }
            }

            // 找患者姓名（从 call_list 取）
            CallList latestCall = callListRepository.findAll().stream()
                .filter(c -> code.equals(c.getPatientCode()))
                .max(java.util.Comparator.comparingInt(c -> c.getId() != null ? c.getId() : 0))
                .orElse(null);
            String patientName = latestCall != null ? latestCall.getPatientName() : code;

            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("patientCode", code);
            row.put("patientName", patientName);
            row.put("completedCount", (int) completed);
            row.put("totalCount", (int) total);
            row.put("pendingCount", (int) pending);
            row.put("progressPct", total > 0 ? (int)(completed * 100 / total) : 0);
            row.put("currentDept", currentDept);
            row.put("currentWaitCount", waitCount);
            int preciseEst = estimatedMin + waitTimeEstimatorService.estimateWaitMinutes(cDeptEntity, waitCount);
            row.put("estimatedFinishMin", preciseEst); 
            row.put("status", completed == 0 ? "未开始" : "进行中");
            result.add(row);
        }

        // 按进度升序（最慢的排前面，优先关注）
        result.sort((a, b) -> (int)a.get("progressPct") - (int)b.get("progressPct"));
        return result;
    }

    // ─────────────────────────────────────────────────────────
    //  P1-5c: 体检套餐-项目关联 CRUD
    // ─────────────────────────────────────────────────────────

    /**
     * 查询某套餐下的检查项目列表（带字典详情）
     * GET /exam/package/{packageId}/items
     */
    @GetMapping("/package/{packageId}/items")
    public ResponseEntity<List<Map<String, Object>>> getPackageItems(@PathVariable Long packageId) {
        List<ExamPackageItem> relations = examPackageItemRepository.findByPackageIdOrderBySortOrderAsc(packageId);
        if (relations.isEmpty()) return ResponseEntity.ok(Collections.emptyList());

        List<Integer> itemIds = relations.stream().map(ExamPackageItem::getItemId).collect(Collectors.toList());
        Map<Integer, DictExamItem> dictMap = dictExamItemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(DictExamItem::getId, d -> d));

        List<Map<String, Object>> result = relations.stream().map(rel -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("relationId", rel.getId());
            row.put("packageId", rel.getPackageId());
            row.put("itemId", rel.getItemId());
            row.put("sortOrder", rel.getSortOrder());
            row.put("remark", rel.getRemark());
            DictExamItem dict = dictMap.get(rel.getItemId());
            if (dict != null) {
                row.put("itemName", dict.getItemName());
                row.put("associatedDeptId", dict.getAssociatedDeptId());
                row.put("isFasting", dict.getIsFasting());
                row.put("genderLimit", dict.getGenderLimit());
                row.put("avgTimeMinutes", dict.getAvgTimeMinutes());
                row.put("dependsOnIds", dict.getDependsOnIds());
            }
            return row;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * 全量覆盖保存某套餐的项目关联（删旧建新，事务保证原子性）
     * POST /exam/package/{packageId}/items/save
     * body: [{"itemId":1,"sortOrder":0,"remark":""}, ...]
     */
    @PostMapping("/package/{packageId}/items/save")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Map<String, Object>> savePackageItems(
            @PathVariable Long packageId,
            @RequestBody List<Map<String, Object>> items) {

        examPackageItemRepository.deleteByPackageId(packageId);
        List<ExamPackageItem> toSave = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> m = items.get(i);
            ExamPackageItem rel = new ExamPackageItem();
            rel.setPackageId(packageId);
            rel.setItemId(Integer.valueOf(m.get("itemId").toString()));
            rel.setSortOrder(m.get("sortOrder") != null ? Integer.valueOf(m.get("sortOrder").toString()) : i);
            rel.setRemark(m.get("remark") != null ? m.get("remark").toString() : "");
            toSave.add(rel);
        }
        examPackageItemRepository.saveAll(toSave);
        log.info("[P1-5] 套餐 {} 关联项目更新完成，共 {} 条", packageId, toSave.size());
        return ResponseEntity.ok(Map.of("success", true, "count", toSave.size()));
    }

    /**
     * 删除单条套餐-项目关联
     * DELETE /exam/package/item/{relationId}
     */
    @DeleteMapping("/package/item/{relationId}")
    public ResponseEntity<Map<String, Object>> deletePackageItem(@PathVariable Long relationId) {
        if (!examPackageItemRepository.existsById(relationId)) {
            return ResponseEntity.ok(Map.of("success", false, "msg", "关联不存在"));
        }
        examPackageItemRepository.deleteById(relationId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 查询所有检查项目字典（供前端双列选择器左侧展示）
     * GET /exam/dict/items
     */
    @GetMapping("/dict/items")
    public ResponseEntity<List<DictExamItem>> getAllDictItems() {
        return ResponseEntity.ok(dictExamItemRepository.findAll());
    }

    /** 兼容前端 POST /exam/getAllItems */
    @PostMapping("/getAllItems")
    public ResponseEntity<List<DictExamItem>> getAllItemsPost() {
        return getAllDictItems();
    }

    @PostMapping("/dict/item/save")
    public ResponseEntity<DictExamItem> saveDictItem(@RequestBody DictExamItem item) {
        return ResponseEntity.ok(dictExamItemRepository.save(item));
    }

    @DeleteMapping("/dict/item/{id}")
    public ResponseEntity<Map<String, Object>> deleteDictItem(@PathVariable Integer id) {
        if (!dictExamItemRepository.existsById(id)) {
            return ResponseEntity.ok(Map.of("success", false));
        }
        dictExamItemRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/packages")
    public ResponseEntity<List<com.gocent.pdjh.entity.ExamPackage>> listPackages() {
        return ResponseEntity.ok(examPackageRepository.findAll());
    }

    @PostMapping("/package/save")
    public ResponseEntity<com.gocent.pdjh.entity.ExamPackage> savePackage(@RequestBody com.gocent.pdjh.entity.ExamPackage pkg) {
        return ResponseEntity.ok(examPackageRepository.save(pkg));
    }

    @DeleteMapping("/package/{id}")
    public ResponseEntity<Map<String, Object>> deletePackage(@PathVariable Long id) {
        examPackageItemRepository.deleteByPackageId(id);
        examPackageRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
