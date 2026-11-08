package com.gocent.pdjh.integration.impl;

import com.gocent.pdjh.integration.HospitalIntegrationService;
import com.gocent.pdjh.integration.model.HisPatientInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 【方案三：老旧系统跨库视图轮询模式 (JDBC DB Polling)】
 * 适用场景：老旧的 HIS 系统没有提供 HTTP 接口的能力，但允许我们在数据库建立只读账号，并创建中间交互表/共享视图。
 * 我们作为服务端，利用定时任务 (Timer/Cron) 不断嗅探中间表，或在挂号时直击其暴露的 Oracle 视图查询。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "integration.mode", havingValue = "jdbc_polling")
public class JdbcPollingIntegrationImpl implements HospitalIntegrationService {

    // private final JdbcTemplate hisJdbcTemplate; // 实际场景应配置第二个用于异构数据库的 JdbcTemplate

    @Override
    public HisPatientInfo getPatientRegistrationInfo(String patientCode) {
        log.info("【JDBC 跨库直读】通过 SQL 语句查杀院方只读视图 V_REGISTRATION，极速提取患者 {} 的挂号记录", patientCode);
        // String sql = "SELECT * FROM his_db.V_REGISTRATION WHERE PATIENT_ID = ?";
        // 返回映射实体...
        HisPatientInfo mock = new HisPatientInfo();
        mock.setPatientCode(patientCode);
        return mock;
    }

    @Override
    public List<String> getPaidExamItemCodes(String patientCode) {
        log.info("【JDBC 跨库直读】扫描老旧平台 HIS 计费底表，确认 {} 已缴费清单", patientCode);
        return new ArrayList<>();
    }

    @Override
    public void syncQueueStateToHis(String patientCode, int state) {
        log.info("【JDBC 跨库直写】直接向院内双方约定的共享中间表执行 UPDATE 操作，强插 {} 状态至 {}", patientCode, state);
        // String sql = "UPDATE his_db.shared_queue_sync SET state = ? WHERE patient_id = ?";
    }
}
