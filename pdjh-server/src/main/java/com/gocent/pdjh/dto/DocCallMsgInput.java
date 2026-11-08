package com.gocent.pdjh.dto;

import lombok.Data;

@Data
public class DocCallMsgInput {
    // 例如："1|接诊室1"
    private String msg;
    private String code;
    private Integer limit;
    private Integer state;
}
