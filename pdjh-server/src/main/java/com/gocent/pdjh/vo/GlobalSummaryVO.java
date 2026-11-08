package com.gocent.pdjh.vo;

import lombok.Data;

@Data
public class GlobalSummaryVO {
    private String deptId;
    private String deptName;
    private Integer waitCount;
    // P2: Actual estimated wait time replacing the frontend "waitCount * 3" logic
    private Integer estimatedWaitMinutes;
    // P2: Actual load heatmap status (e.g., IDLE, BUSY, CONGESTED)
    private String status;
}
