package com.gocent.pdjh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Date;

/**
 * 独立叫号音响设备台账。
 * 与候诊大屏的绑定在大屏侧维护（voice_scope_mapping scopeType=SCREEN）；
 * 播报文案使用触发叫号的大屏 CallContent。
 */
@Data
@Entity
@Table(name = "voice_device")
public class VoiceDevice {

    @Id
    @Column(length = 36)
    private String id;

    /** 设备编号（WS 订阅 /topic/voice/{deviceCode}） */
    @Column(name = "device_code", length = 64, unique = true, nullable = false)
    private String deviceCode;

    @Column(length = 100)
    private String name;

    @Column(length = 200)
    private String location;

    /**
     * 旧版：音响侧绑定的单一大屏（已废弃，改由大屏端多对多绑定）。
     * 播报解析仍兼容读取，新配置请写入 voice_scope_mapping(SCREEN)。
     */
    @Column(name = "screen_id", length = 64)
    private String screenId;

    /**
     * 播报模式：WS=音响终端订阅 WebSocket；HTTP=向 endpointUrl POST 叫号文本；BOTH=同时
     */
    @Column(name = "play_mode", length = 16)
    private String playMode = "WS";

    /** HTTP 推送地址（playMode 含 HTTP 时必填） */
    @Column(name = "endpoint_url", length = 500)
    private String endpointUrl;

    /** 1=启用 0=停用 */
    private Integer state = 1;

    @Column(name = "create_date")
    private Date createDate;
}
