package com.gocent.pdjh.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class SignDeviceOutput {
    private String id;
    private String deviceCode;
    private String name;
    private String title;
    private String location;
    private String showText;
    private Integer status;
    /** 配置状态文案：启用 / 待配置 / 停用 */
    private String statusModel;
    /** 是否已绑定诊室等配置完整 */
    private Boolean configComplete;
    /** 终端 5 分钟内 Register/Resolve/GetDeviceConfig 视为已连接 */
    private Boolean online;
    private Date lastSeenAt;
    private Date createDate;
    private String boundRoomSummary;
    private List<OfficeRoomBrief> boundRooms;
}
