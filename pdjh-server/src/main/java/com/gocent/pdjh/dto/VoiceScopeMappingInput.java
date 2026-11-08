package com.gocent.pdjh.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class VoiceScopeMappingInput {
    private String voiceDeviceId;
    /** 诊室 ID 列表 */
    private List<String> officeRoomIds = new ArrayList<>();
    /** 诊区/科室 ID 列表（该科下属诊室叫号也会推送） */
    private List<String> deptIds = new ArrayList<>();
}
