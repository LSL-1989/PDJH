package com.gocent.pdjh.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class DepartmentOutput {
    @JsonProperty("ID")
    private String id;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("Type")
    private Integer type;
    @JsonProperty("State")
    private Integer state;
    @JsonProperty("FatherID")
    private String fatherID;
    @JsonProperty("Queue")
    private String queue;
    @JsonProperty("Disabled")
    private Boolean disabled;
    @JsonProperty("UserName")
    private String userName;
    @JsonProperty("BusinessType")
    private String businessType;

    @JsonProperty("IsSuspended")
    private Integer isSuspended;

    @JsonProperty("MedTechSubType")
    private String medTechSubType;

    /** 无子节点时可挂医生/物理诊室 */
    @JsonProperty("IsLeaf")
    private Boolean isLeaf;

    @JsonProperty("HasChildren")
    private Boolean hasChildren;
}
