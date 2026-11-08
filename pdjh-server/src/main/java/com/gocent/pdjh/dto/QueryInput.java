package com.gocent.pdjh.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class QueryInput {
    @JsonProperty("PatientCode")
    private String patientCode;
    
    @JsonProperty("PatientName")
    private String patientName;
    
    // 前端传来可能是单个 ID 的字符串，也可能是数组（联级下拉）
    // 为了兼容，使用 Object 类型但在 Controller 手动解析，或者用 List 甚至 String。
    // 我们原版 vue 中是 cascader: v-model="QueryInput.DeptID"，对于 cascader 就是一个 Array
    @JsonProperty("DeptID")
    private Object deptID;
    
    @JsonProperty("Doctors")
    private String doctors;
    
    @JsonProperty("CallTime")
    private String callTime;
    
    @JsonProperty("PageIndex")
    private Integer pageIndex = 1;
    
    @JsonProperty("PageSize")
    private Integer pageSize = 10;
    
    @JsonProperty("businessType")
    private String businessType;
}
