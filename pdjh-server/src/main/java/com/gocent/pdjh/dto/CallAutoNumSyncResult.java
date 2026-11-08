package com.gocent.pdjh.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 编号规则同步结果：按角色主任工作台权限汇总 call_list 当日发号并写入 call_auto_num */
@Data
public class CallAutoNumSyncResult {

    @JsonProperty("SyncDate")
    private String syncDate;

    /** 本次纳入同步的业务类型（如 OUTPATIENT、PHARMACY） */
    @JsonProperty("BusinessTypes")
    private List<String> businessTypes = new ArrayList<>();

    @JsonProperty("DeptCount")
    private int deptCount;

    @JsonProperty("CreatedCount")
    private int createdCount;

    @JsonProperty("UpdatedCount")
    private int updatedCount;

    @JsonProperty("SyncedCount")
    private int syncedCount;
}
