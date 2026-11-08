package com.gocent.pdjh.dto;

import lombok.Data;
import java.util.List;

@Data
public class ExamDashboardOutput {
    private ExamItemDto current;
    private List<ExamItemDto> pending;
    private List<ExamItemDto> completed;
    
    @Data
    public static class ExamItemDto {
        private Integer id;
        private Integer itemId;
        private String itemName;
        private Integer isFasting;
        private String suggestedDeptName;
        private Integer waitingCount;
    }
}
