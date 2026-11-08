package com.gocent.pdjh.dto;

import lombok.Data;
import java.util.List;

@Data
public class ScreenDeptMappingInput {
    // 诊室 ID 列表
    private List<String> departmentInputKeys;
    // 屏幕 ID
    private String screenInput;
    
    // 兼容原系统结构保留
    private DepartmentInput departmentInput;
    private ScreensInput screensInput;
}
