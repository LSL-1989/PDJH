package com.gocent.pdjh.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PasswordChangeInput {
    @JsonProperty("UserNo")
    private String userNo;
    @JsonProperty("OldPwd")
    private String oldPwd;
    @JsonProperty("NewPwd")
    private String newPwd;
}
