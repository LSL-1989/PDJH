package com.gocent.pdjh.event;

import org.springframework.context.ApplicationEvent;

public class ExamItemCompletedEvent extends ApplicationEvent {
    private final String patientCode;
    private final String examBatchNo;
    private final Integer completedItemId;

    public ExamItemCompletedEvent(Object source, String patientCode, String examBatchNo, Integer completedItemId) {
        super(source);
        this.patientCode = patientCode;
        this.examBatchNo = examBatchNo;
        this.completedItemId = completedItemId;
    }

    public String getPatientCode() {
        return patientCode;
    }

    public String getExamBatchNo() {
        return examBatchNo;
    }

    public Integer getCompletedItemId() {
        return completedItemId;
    }
}
