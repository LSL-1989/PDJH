package com.gocent.pdjh.integration.impl;

import com.gocent.pdjh.integration.HospitalIntegrationService;
import com.gocent.pdjh.integration.model.HisPatientInfo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "integration.mode", havingValue = "http_active")
public class HttpActiveIntegrationImpl implements HospitalIntegrationService {

    @Override
    @CircuitBreaker(name = "hisClient", fallbackMethod = "fallbackGetPatientRegistrationInfo")
    public HisPatientInfo getPatientRegistrationInfo(String patientCode) {
        log.info("【主动 HTTP 调用】向 HIS 系统请求患者 {} 的挂号数据...", patientCode);
        
        // 此处为模拟调用外网逻辑。若在此抛出异常或超时，Resilience4j自动介入。
        HisPatientInfo info = new HisPatientInfo();
        info.setPatientCode(patientCode);
        info.setIsAppointment(1);
        return info;
    }

    public HisPatientInfo fallbackGetPatientRegistrationInfo(String patientCode, Throwable t) {
        log.error("【熔断降级】获取 HIS 个档网络拥塞被切断，触发防雪崩熔断，患者：{}, 拦截原因：{}", patientCode, t.getMessage());
        return null; // 拒绝包装脏名落库，向外反馈 null 让消费者或前端做对应的挂起防呆阻断
    }

    @Override
    @CircuitBreaker(name = "hisClient", fallbackMethod = "fallbackGetPaidExamItemCodes")
    public List<String> getPaidExamItemCodes(String patientCode) {
        log.info("【主动 HTTP 调用】向结算中心请求患者 {} 的已缴费体检清单...", patientCode);
        return new ArrayList<>();
    }

    public List<String> fallbackGetPaidExamItemCodes(String patientCode, Throwable t) {
        log.error("【熔断降级】获取体检清单拥塞拦截，患者：{}", patientCode);
        return new ArrayList<>();
    }

    @Override
    @CircuitBreaker(name = "hisClient", fallbackMethod = "fallbackSyncQueueState")
    public void syncQueueStateToHis(String patientCode, int state) {
        log.info("【主动 HTTP 调用】向 HIS 中心大屏系统推送状态回执: {} 状态转为 {}", patientCode, state);
    }
    
    public void fallbackSyncQueueState(String patientCode, int state, Throwable t) {
        log.error("【熔断降级】向 HIS 系统同步状态回执因拥塞被抛弃，稍后可利用 DLQ 重试，患者：{}", patientCode);
    }
}
