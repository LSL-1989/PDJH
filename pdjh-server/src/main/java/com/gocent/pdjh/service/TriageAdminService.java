package com.gocent.pdjh.service;

public interface TriageAdminService {
    boolean revive(Integer callListId);
    boolean forceSwap(Integer callListId, String newDeptId, String newDoctorId);
    boolean manualCutIn(Integer callListId, Integer targetXh);
}
