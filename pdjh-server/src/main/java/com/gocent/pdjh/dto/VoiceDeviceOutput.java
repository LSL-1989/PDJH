package com.gocent.pdjh.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class VoiceDeviceOutput {
    private String id;
    private String deviceCode;
    private String name;
    private String location;
    private String playMode;
    private String endpointUrl;
    private Integer state;
    private Date createDate;
    /** 绑定的候诊大屏 ID */
    private String screenId;
    /** 绑定大屏名称 */
    private String screenName;
    /** 大屏语音播报模板（CallContent） */
    private String callContent;
    /** 绑定摘要 */
    private String boundSummary;
    /** @deprecated 兼容旧版诊室/诊区绑定展示 */
    private List<String> officeRoomIds = new ArrayList<>();
    /** @deprecated 兼容旧版 */
    private List<String> deptIds = new ArrayList<>();
}
