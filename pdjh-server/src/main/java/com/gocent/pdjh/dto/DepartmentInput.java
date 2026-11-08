package com.gocent.pdjh.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class DepartmentInput {
    @JsonProperty("ID")
    private String id;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("State")
    private Integer state;
    @JsonProperty("Queue")
    private String queue;
    @JsonProperty("FatherID")
    private String fatherID;
    @JsonProperty("Type")
    private Integer type;
    @JsonProperty("BusinessType")
    private String businessType;

    @JsonProperty("IsSuspended")
    private Integer isSuspended;

    @JsonProperty("MedTechSubType")
    private String medTechSubType;
}
