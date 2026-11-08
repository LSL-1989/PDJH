package com.gocent.pdjh.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "hj_implement_task")
public class ImplementTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 步骤唯一编码，例如 STEP1_DB_INIT
     */
    @Column(name = "step_code", length = 64)
    private String stepCode;

    /**
     * 所属大洲阶段名，例如 1_SYSTEM_SETUP
     */
    @Column(name = "phase_code", length = 64)
    private String phaseCode;

    /**
     * 步骤具名，例如 数据库连通性检查
     */
    @Column(name = "step_name", length = 128)
    private String stepName;

    /**
     * 实施人员名称
     */
    @Column(name = "implementer", length = 64)
    private String implementer;

    /**
     * 本环节耗时(分钟)
     */
    @Column(name = "time_spent_minutes")
    private Integer timeSpentMinutes;

    /**
     * 实施过程笔记/反推异常记录
     */
    @Column(name = "remark", length = 1000)
    private String remark;

    /**
     * 状态: 0未开始, 1进行中, 2已完成并验收
     */
    @Column(name = "status")
    private Integer status;

    @Column(name = "start_time")
    private Date startTime;

    @Column(name = "finish_time")
    private Date finishTime;

    @Column(name = "create_time")
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStepCode() {
        return stepCode;
    }

    public void setStepCode(String stepCode) {
        this.stepCode = stepCode;
    }

    public String getPhaseCode() {
        return phaseCode;
    }

    public void setPhaseCode(String phaseCode) {
        this.phaseCode = phaseCode;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getImplementer() {
        return implementer;
    }

    public void setImplementer(String implementer) {
        this.implementer = implementer;
    }

    public Integer getTimeSpentMinutes() {
        return timeSpentMinutes;
    }

    public void setTimeSpentMinutes(Integer timeSpentMinutes) {
        this.timeSpentMinutes = timeSpentMinutes;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
