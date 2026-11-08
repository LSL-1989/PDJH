package com.gocent.pdjh.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gocent.pdjh.entity.Department;
import com.gocent.pdjh.entity.SysConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WaitTimeEstimatorService {

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 根据患者前方排队人数和科室参数估算等候时间
     * @param dept          患者所在的科室/队列
     * @param waitPosition  排在患者前面的在等候的总人数
     * @return 预计等候分钟数
     */
    public int estimateWaitMinutes(Department dept, int waitPosition) {
        if (waitPosition <= 0) return 0;
        if (dept == null) return waitPosition * 5;

        int avgTimePerPerson = 5; // 默认5分钟/人
        int concurrentWindows = 1; // 默认1个窗口并发

        try {
            if ("OUTPATIENT".equals(dept.getBusinessType())) {
                // 门诊
                avgTimePerPerson = 8;
                concurrentWindows = 1; // 单个诊室串行
            } else if ("PHARMACY".equals(dept.getBusinessType())) {
                // 药房
                avgTimePerPerson = 2; // 配药较快
                concurrentWindows = 3; 
            } else {
                // 医技科室或体检
                SysConfig medtechConfig = sysConfigService.getConfigByKey("RULE_MEDICAL_TECH");
                if (medtechConfig != null && medtechConfig.getConfigValue() != null) {
                    JsonNode root = objectMapper.readTree(medtechConfig.getConfigValue());
                    String deptName = dept.getName() != null ? dept.getName() : "";
                    
                    if (deptName.contains("检验") || deptName.contains("抽血")) {
                        if (root.has("lab")) {
                            JsonNode lab = root.get("lab");
                            avgTimePerPerson = 3; // 采血时间短
                            concurrentWindows = lab.has("windowCount") ? lab.get("windowCount").asInt(6) : 6;
                        }
                    } else if (deptName.contains("超声") || deptName.contains("B超") || deptName.contains("彩超")) {
                        if (root.has("ultrasound")) {
                            JsonNode us = root.get("ultrasound");
                            avgTimePerPerson = us.has("avgTimeAbdomen") ? us.get("avgTimeAbdomen").asInt(20) : 20; 
                            concurrentWindows = us.has("roomCount") ? us.get("roomCount").asInt(4) : 4;
                        }
                    } else if (deptName.contains("CT") || deptName.contains("放射") || deptName.contains("X光") || deptName.contains("磁共振") || deptName.contains("MRI")) {
                        if (root.has("radiology")) {
                            JsonNode rad = root.get("radiology");
                            avgTimePerPerson = 15; // 放射检查均值
                            concurrentWindows = rad.has("ctMachineCount") ? rad.get("ctMachineCount").asInt(2) : 2;
                        }
                    } else if (deptName.contains("心电")) {
                        if (root.has("ecg")) {
                            JsonNode ecg = root.get("ecg");
                            avgTimePerPerson = ecg.has("avgExamMin") ? ecg.get("avgExamMin").asInt(6) : 6;
                            concurrentWindows = ecg.has("bedCount") ? ecg.get("bedCount").asInt(3) : 3;
                        }
                    }
                }
            }

            // 防止窗口数为0导致触发除零异常
            if (concurrentWindows <= 0) concurrentWindows = 1;

            // 计算时间: (前序人数 / 窗口并发数)向上取整 × 单人均耗时
            double batches = Math.ceil((double) waitPosition / concurrentWindows);
            int est = (int) (batches * avgTimePerPerson);
            
            // 加上 2 分钟缓冲切换时间 (含通知、核对等开销)
            return est + 2;

        } catch (Exception e) {
            // 发生解析错误，返回保守退化值
            return waitPosition * 5; 
        }
    }
}
