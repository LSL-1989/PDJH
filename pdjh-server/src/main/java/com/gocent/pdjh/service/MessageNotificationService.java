package com.gocent.pdjh.service;

/**
 * 院内消息网关/智慧医院平台对接预留接口
 * 用于在排队关键节点向患者或医护推送提醒 (如微信公众号/小程序、短信、院内 APP 等)
 */
public interface MessageNotificationService {

    /**
     * 候诊阈值提醒 (如：前方还有 N 人，请尽早前往候诊区)
     * @param patientCode 患者编号
     * @param deptName 候诊科室
     * @param waitCount 前方排队人数
     * @param estimatedMinutes 预计还需等待时间
     */
    void sendQueueReminder(String patientCode, String deptName, int waitCount, int estimatedMinutes);

    /**
     * 诊毕流转/导诊提醒 (如：您已完成 [抽血]，请前往 [B超室] 等候)
     * @param patientCode 患者编号
     * @param completedItem 刚刚完成的项目
     * @param nextDeptName 下一个需要前往的科室 (为 null 代表全部完成)
     */
    void sendRoutingReminder(String patientCode, String completedItem, String nextDeptName);
    
    /**
     * 业务熔断/改签告警提醒
     * @param patientCode 患者编号
     * @param originalDept 原计划科室
     * @param message 变动原因及指引
     */
    void sendEmergencyAlert(String patientCode, String originalDept, String message);
}
