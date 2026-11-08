package com.gocent.pdjh.event;

import org.springframework.context.ApplicationEvent;

public class PatientSignInEvent extends ApplicationEvent {
    private final String patientCode;

    public PatientSignInEvent(Object source, String patientCode) {
        super(source);
        this.patientCode = patientCode;
    }

    public String getPatientCode() {
        return patientCode;
    }
}
